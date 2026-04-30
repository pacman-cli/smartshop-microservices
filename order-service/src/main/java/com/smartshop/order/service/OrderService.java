package com.smartshop.order.service;

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
import com.smartshop.order.event.OrderCreatedEvent;
import com.smartshop.order.event.OrderEventProducer;
import com.smartshop.order.exception.OrderNotFoundException;
import com.smartshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final OrderEventProducer orderEventProducer;

    /**
     * Create a new order:
     * 1. Validate user exists (via user-service)
     * 2. Validate and fetch product details (via product-service)
     * 3. Atomically reduce stock for all products in a single batch call
     * 4. Save the order
     * 5. Publish order-created event to Kafka
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Check for existing orders with same idempotency key
        if (request.getIdempotencyKey() != null) {
            orderRepository.findByOrderNumber(request.getIdempotencyKey())
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Order with this idempotency key already exists: " + request.getIdempotencyKey());
            });
        }

        // 1. Validate user
        UserResponse user = userServiceClient.getUserById(request.getUserId());

        // 2. Build order - use idempotency key if provided
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

        // 3. Fetch product details in batch and build order items + stock reduction list
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .toList();
        Map<Long, ProductResponse> productMap = productServiceClient.getProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductResponse::getId, p -> p));

        List<StockItem> stockItems = new ArrayList<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product = productMap.get(itemRequest.getProductId());

            stockItems.add(StockItem.builder()
                    .productId(product.getId())
                    .quantity(itemRequest.getQuantity())
                    .build());

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build();

            order.addItem(item);
        }

        // 4. Atomically reduce stock for ALL items (all-or-nothing)
        productServiceClient.batchReduceStock(
                BatchStockRequest.builder().items(stockItems).build());

        // 5. Calculate total and save
        order.calculateTotalAmount();
        Order savedOrder = orderRepository.save(order);

        log.info("Order created: {} (total: {})", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        // 6. Publish event (async, non-blocking)
        try {
            orderEventProducer.publishOrderCreated(OrderCreatedEvent.builder()
                    .orderNumber(savedOrder.getOrderNumber())
                    .userId(savedOrder.getUserId())
                    .userEmail(savedOrder.getUserEmail())
                    .totalAmount(savedOrder.getTotalAmount())
                    .status(savedOrder.getStatus().name())
                    .itemCount(savedOrder.getItems().size())
                    .createdAt(savedOrder.getCreatedAt())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish order created event for order: {}", savedOrder.getOrderNumber(), e);
            // Don't fail the order creation if Kafka is down
        }

        return mapToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        return mapToResponse(order);
    }

    public Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return orderRepository.findByUserId(userId,
                        PageRequest.of(normalizedPage, normalizedSize,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    public Page<OrderResponse> getAllOrders(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return orderRepository.findAll(
                        PageRequest.of(normalizedPage, normalizedSize,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        log.info("Order {} status updated to {}", updated.getOrderNumber(), newStatus);
        return mapToResponse(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        // Restore stock for all items in the cancelled order
        restoreStockForOrder(updated);

        log.info("Order cancelled: {}", updated.getOrderNumber());
        return mapToResponse(updated);
    }

    /**
     * Restore stock for all items in a cancelled/failed order using batch endpoint.
     * Best-effort: failure is logged but doesn't block the cancellation.
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
                .items(order.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
