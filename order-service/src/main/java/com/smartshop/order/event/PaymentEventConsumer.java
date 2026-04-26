package com.smartshop.order.event;

import com.smartshop.order.client.BatchStockRequest;
import com.smartshop.order.client.ProductServiceClient;
import com.smartshop.order.client.StockItem;
import com.smartshop.order.entity.Order;
import com.smartshop.order.entity.OrderStatus;
import com.smartshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka consumer that listens for payment.completed events
 * and updates order status accordingly.
 *
 * - COMPLETED payment -> Order status = CONFIRMED
 * - FAILED payment    -> Order status = PAYMENT_FAILED + restore stock
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "order-group",
            properties = {
                "spring.json.trusted.packages=*",
                "spring.json.value.default.type=com.smartshop.order.event.PaymentCompletedEvent"
            })
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment event for order: {} status: {}",
                event.getOrderNumber(), event.getStatus());

        Order order = orderRepository.findByOrderNumber(event.getOrderNumber())
                .orElse(null);

        if (order == null) {
            log.error("Order not found for payment event: {}", event.getOrderNumber());
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not PENDING (current: {}), skipping payment update",
                    event.getOrderNumber(), order.getStatus());
            return;
        }

        if ("COMPLETED".equals(event.getStatus())) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} confirmed after successful payment (txn: {})",
                    event.getOrderNumber(), event.getTransactionId());

        } else if ("FAILED".equals(event.getStatus())) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            log.warn("Order {} marked as PAYMENT_FAILED (reason: {})",
                    event.getOrderNumber(), event.getFailureReason());

            // Restore stock for all items in the order
            restoreStockForOrder(order);
        }
    }

    /**
     * Restore stock for all items in a failed/cancelled order using batch endpoint.
     * Best-effort: if product-service is down, we log the error
     * but don't fail the status update.
     */
    private void restoreStockForOrder(Order order) {
        try {
            List<StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            productServiceClient.batchRestoreStock(
                    BatchStockRequest.builder().items(stockItems).build());
            log.info("Stock restored for all {} items in order {}",
                    stockItems.size(), order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to batch restore stock for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }
}
