package com.smartshop.payment.service;

import com.smartshop.payment.dto.PaymentRequest;
import com.smartshop.payment.dto.PaymentResponse;
import com.smartshop.payment.entity.Payment;
import com.smartshop.payment.entity.PaymentStatus;
import com.smartshop.payment.event.PaymentCompletedEvent;
import com.smartshop.payment.event.PaymentEventProducer;
import com.smartshop.payment.exception.PaymentNotFoundException;
import com.smartshop.payment.exception.PaymentProcessingException;
import com.smartshop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Service — simulates payment processing.
 *
 * In a real system, this would integrate with Stripe, PayPal, etc.
 * For this demo, we simulate: 90% success, 10% failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderNumber());

        // Check if payment already exists for this order
        if (paymentRepository.findByOrderNumber(request.getOrderNumber()).isPresent()) {
            throw new PaymentProcessingException(
                    "Payment already exists for order: " + request.getOrderNumber());
        }

        Payment payment = Payment.builder()
                .transactionId(generateTransactionId())
                .orderNumber(request.getOrderNumber())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PROCESSING)
                .build();

        // Simulate payment processing (90% success rate)
        boolean success = simulatePaymentGateway();

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            log.info("Payment completed for order: {} (txn: {})",
                    request.getOrderNumber(), payment.getTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment gateway declined the transaction");
            log.warn("Payment failed for order: {} (txn: {})",
                    request.getOrderNumber(), payment.getTransactionId());
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Publish event to Kafka
        try {
            paymentEventProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                    .transactionId(savedPayment.getTransactionId())
                    .orderNumber(savedPayment.getOrderNumber())
                    .userId(savedPayment.getUserId())
                    .userEmail(request.getUserEmail())
                    .amount(savedPayment.getAmount())
                    .status(savedPayment.getStatus().name())
                    .paymentMethod(savedPayment.getPaymentMethod().name())
                    .failureReason(savedPayment.getFailureReason())
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish payment event for order: {}", request.getOrderNumber(), e);
        }

        return mapToResponse(savedPayment);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + transactionId));
        return mapToResponse(payment);
    }

    public PaymentResponse getPaymentByOrderNumber(String orderNumber) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for order: " + orderNumber));
        return mapToResponse(payment);
    }

    public Page<PaymentResponse> getPaymentsByUserId(Long userId, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return paymentRepository.findByUserId(userId,
                        PageRequest.of(normalizedPage, normalizedSize,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    @Transactional
    public PaymentResponse refundPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentProcessingException(
                    "Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment refunded = paymentRepository.save(payment);

        log.info("Payment refunded: {} (order: {})", refunded.getTransactionId(), refunded.getOrderNumber());
        return mapToResponse(refunded);
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    /**
     * Simulate a payment gateway: 90% success, 10% failure.
     */
    private boolean simulatePaymentGateway() {
        return Math.random() < 0.9;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .orderNumber(payment.getOrderNumber())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod().name())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
