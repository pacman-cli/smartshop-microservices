package com.smartshop.payment.service;

import com.smartshop.payment.dto.PaymentRequest;
import com.smartshop.payment.dto.PaymentResponse;
import com.smartshop.payment.entity.Payment;
import com.smartshop.payment.entity.PaymentMethod;
import com.smartshop.payment.entity.PaymentStatus;
import com.smartshop.payment.event.PaymentEventProducer;
import com.smartshop.payment.exception.PaymentNotFoundException;
import com.smartshop.payment.exception.PaymentProcessingException;
import com.smartshop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPaymentCreatesPayment() {
        PaymentRequest request = PaymentRequest.builder()
                .orderNumber("ORD-001")
                .userId(100L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .userEmail("test@example.com")
                .build();

        when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            payment.setCreatedAt(LocalDateTime.now());
            return payment;
        });

        PaymentResponse result = paymentService.processPayment(request);

        assertThat(result.getTransactionId()).isNotNull();
        assertThat(result.getTransactionId()).startsWith("TXN-");
        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
        // Status is either COMPLETED or FAILED (90/10 simulation)
        assertThat(result.getStatus()).isIn(
                PaymentStatus.COMPLETED.name(), PaymentStatus.FAILED.name());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPaymentRejectsDuplicateOrder() {
        PaymentRequest request = PaymentRequest.builder()
                .orderNumber("ORD-001")
                .userId(100L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        Payment existing = Payment.builder()
                .id(1L).orderNumber("ORD-001").build();

        when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(PaymentProcessingException.class);
    }

    @Test
    void getPaymentByIdReturnsResponse() {
        Payment payment = Payment.builder()
                .id(1L)
                .transactionId("TXN-001")
                .orderNumber("ORD-001")
                .userId(100L)
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getPaymentByIdThrowsWhenNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void getPaymentByOrderNumberReturnsResponse() {
        Payment payment = Payment.builder()
                .id(1L)
                .transactionId("TXN-001")
                .orderNumber("ORD-001")
                .userId(100L)
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(payment));

        PaymentResponse result = paymentService.getPaymentByOrderNumber("ORD-001");

        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
    }

    @Test
    void getPaymentsByUserIdReturnsPagedResults() {
        Payment payment = Payment.builder()
                .id(1L).orderNumber("ORD-001").userId(100L)
                .transactionId("TXN-001").amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED).paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        Page<Payment> page = new PageImpl<>(List.of(payment));
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(paymentRepository.findByUserId(100L, pageRequest)).thenReturn(page);

        Page<PaymentResponse> result = paymentService.getPaymentsByUserId(100L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void refundPaymentSetsRefundedStatus() {
        Payment payment = Payment.builder()
                .id(1L).transactionId("TXN-001").orderNumber("ORD-001")
                .userId(100L).amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED).paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse result = paymentService.refundPayment(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED.name());
    }

    @Test
    void refundPaymentRejectsNonCompletedPayment() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.PENDING).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L))
                .isInstanceOf(PaymentProcessingException.class);
    }
}
