package com.smartshop.contracts.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Canonical event published to Kafka topic {@code order.created}
 * when a new order is placed.
 *
 * <p><b>Producer:</b> order-service</p>
 * <p><b>Consumers:</b> notification-service (email confirmation)</p>
 *
 * <p>This is the single source of truth for the order-created contract.
 * Do NOT create local copies in individual services.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String orderNumber;
    private Long userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String status;
    private int itemCount;
    private LocalDateTime createdAt;
}
