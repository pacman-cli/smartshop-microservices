package com.smartshop.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event received from Kafka when a payment is completed (or fails).
 * Published by payment-service, consumed here to update order status.
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
    private String status;
    private String paymentMethod;
    private String failureReason;
    private LocalDateTime processedAt;
}
