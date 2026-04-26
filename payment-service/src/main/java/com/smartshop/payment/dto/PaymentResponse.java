package com.smartshop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private String transactionId;
    private String orderNumber;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String failureReason;
    private LocalDateTime createdAt;
}
