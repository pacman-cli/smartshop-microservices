package com.smartshop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.contracts.audit.BaseAuditEntity;
import com.smartshop.contracts.event.OrderCreatedEvent;
import com.smartshop.contracts.event.PaymentCompletedEvent;
import com.smartshop.order.client.BatchStockRequest;
import com.smartshop.order.client.ProductResponse;
import com.smartshop.order.client.ProductServiceClient;
import com.smartshop.order.client.StockItem;
import com.smartshop.order.client.UserResponse;
import com.smartshop.order.client.UserServiceClient;
import com.smartshop.order.dto.OrderItemRequest;
import com.smartshop.order.dto.OrderItemResponse;
import com.smartshop.order.dto.OrderRequest;
import com.smartshop.order.dto.OrderResponse;
import com.smartshop.order.entity.Order;
import com.smartshop.order.entity.OrderItem;
import com.smartshop.order.entity.OrderStatus;
import com.smartshop.order.entity.OutboxEvent;
import com.smartshop.order.exception.OrderNotFoundException;
import com.smartshop.order.repository.OrderRepository;
import com.smartshop.order.repository.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        Long authenticatedUserId;
        try {
            authenticatedUserId = Long.parseLong(principal);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in security context: {}", principal);
            throw new RuntimeException("Authentication error: missing or invalid user ID");
        }
        
        log.info("Creating order for user: {}", authenticatedUserId);

        if (request.getIdempotencyKey() != null) {
            orderRepository.findByOrderNumber(request.getIdempotencyKey())
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Order with this idempotency key already exists: " + request.getIdempotencyKey());
            });
        }

        UserResponse user = userServiceClient.getUserById(authenticatedUserId);

        String orderNumber = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(user.getId())
                .userEmail(user.getEmail())
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .build();

        Map<Long, Integer> productQuantities = request.getItems().stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::getProductId,
                        Collectors.summingInt(OrderItemRequest::getQuantity)
                ));

        List<Long> productIds = productQuantities.keySet().stream().toList();
        Map<Long, ProductResponse> productMap = productServiceClient.getProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductResponse::getId, p -> p));

        List<StockItem> stockItems = new ArrayList<>();
        
        productQuantities.forEach((productId, quantity) -> {
            ProductResponse product = productMap.get(productId);
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            stockItems.add(StockItem.builder()
                    .productId(product.getId())
                    .quantity(quantity)
                    .build());

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(quantity)
                    .price(product.getPrice())
                    .build();

            order.addItem(item);
        });

        productServiceClient.batchReduceStock(
                BatchStockRequest.builder()
                        .idempotencyKey(orderNumber)
                        .items(stockItems)
                        .build());

        order.calculateTotalAmount();
        Order savedOrder = orderRepository.save(order);

        log.info("Order saved: {} (total: {})", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        meterRegistry.counter("smartshop.orders.created", "status", savedOrder.getStatus().name()).increment();
        meterRegistry.gauge("smartshop.orders.revenue", savedOrder.getTotalAmount().doubleValue());

        saveOrderCreatedToOutbox(savedOrder);

        return mapToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    public Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserId(userId, pageRequest)
                .map(this::mapToResponse);
    }

    public Page<OrderResponse> getAllOrders(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(pageRequest)
                .map(this::mapToResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        meterRegistry.counter("smartshop.orders.status.update", "status", status.name()).increment();
        return mapToResponse(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        
        // Restore stock
        List<StockItem> itemsToRestore = order.getItems().stream()
                .map(item -> StockItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        productServiceClient.batchRestoreStock(
                BatchStockRequest.builder()
                        .idempotencyKey(order.getOrderNumber())
                        .items(itemsToRestore)
                        .build());

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional
    public void handlePaymentResult(PaymentCompletedEvent event) {
        log.info("Handling payment result for order: {}", event.getOrderNumber());
        Order order = orderRepository.findByOrderNumber(event.getOrderNumber())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderNumber()));
        
        order.setStatus(OrderStatus.COMPLETED); // Assuming PAID maps to COMPLETED in this context or use custom status
        orderRepository.save(order);
        meterRegistry.counter("smartshop.orders.completed").increment();
    }

    private void saveOrderCreatedToOutbox(Order order) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId())
                    .userEmail(order.getUserEmail())
                    .totalAmount(order.getTotalAmount())
                    .createdAt(order.getCreatedAt())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(order.getOrderNumber())
                    .aggregateType("ORDER")
                    .eventType("ORDER_CREATED")
                    .payload(payload)
                    .status("PENDING")
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new RuntimeException("Persistence error", e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(this::mapItemToResponse).toList())
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
