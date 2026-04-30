package com.smartshop.order.client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentServiceResponse {
    private Long id;
    private String transactionId;
    private String orderNumber;
    private String status;
    private String failureReason;
}