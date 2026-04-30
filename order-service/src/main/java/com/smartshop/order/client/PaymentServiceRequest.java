package com.smartshop.order.client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentServiceRequest {
    private String orderNumber;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String userEmail;
}