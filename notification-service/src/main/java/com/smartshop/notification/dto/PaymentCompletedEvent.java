package com.smartshop.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors the PaymentCompletedEvent published by payment-service.
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
