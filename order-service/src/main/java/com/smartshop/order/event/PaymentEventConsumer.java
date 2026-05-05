package com.smartshop.order.event;

import com.smartshop.contracts.event.PaymentCompletedEvent;
import com.smartshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for payment.completed events
 * and delegates to OrderService for status updates.
 *
 * <p>This listener is intentionally thin — business logic
 * (status transitions, stock restoration) lives in OrderService.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "order-group",
            properties = {
                "spring.json.trusted.packages=com.smartshop.contracts",
                "spring.json.value.default.type=com.smartshop.contracts.event.PaymentCompletedEvent"
            })
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment event for order: {} status: {}",
                event.getOrderNumber(), event.getStatus());

        orderService.handlePaymentResult(event);
    }
}
