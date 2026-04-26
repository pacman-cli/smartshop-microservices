package com.smartshop.payment.controller;

import com.smartshop.payment.dto.PaymentRequest;
import com.smartshop.payment.dto.PaymentResponse;
import com.smartshop.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable @Min(1) Long id) {
        return paymentService.getPaymentById(id);
    }

    @GetMapping(params = "transactionId")
    public PaymentResponse getPaymentByTransactionId(@RequestParam String transactionId) {
        return paymentService.getPaymentByTransactionId(transactionId);
    }

    @GetMapping(params = "orderNumber")
    public PaymentResponse getPaymentByOrderNumber(@RequestParam String orderNumber) {
        return paymentService.getPaymentByOrderNumber(orderNumber);
    }

    @GetMapping(params = "userId")
    public Page<PaymentResponse> getPaymentsByUserId(
            @RequestParam @Min(1) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return paymentService.getPaymentsByUserId(userId, page, size);
    }

    @PatchMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable @Min(1) Long id) {
        return paymentService.refundPayment(id);
    }
}
