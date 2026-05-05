package com.smartshop.contracts.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Canonical event published to Kafka topic {@code payment.completed}
 * when a payment succeeds or fails.
 *
 * <p><b>Producer:</b> payment-service</p>
 * <p><b>Consumers:</b> order-service (status update), notification-service (email)</p>
 *
 * <p>This is the single source of truth for the payment-completed contract.
 * Do NOT create local copies in individual services.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private String transactionId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private BigDecimal amount;
    /** "COMPLETED" or "FAILED" */
    private String status;
    private String paymentMethod;
    private String failureReason;
    private LocalDateTime processedAt;
}
