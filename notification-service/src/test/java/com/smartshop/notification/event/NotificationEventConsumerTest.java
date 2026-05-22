package com.smartshop.notification.event;

import com.smartshop.contracts.event.OrderCreatedEvent;
import com.smartshop.contracts.event.PaymentCompletedEvent;
import com.smartshop.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void handleOrderCreatedSendsConfirmation() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderNumber("ORD-001")
                .userEmail("user@test.com")
                .totalAmount(new BigDecimal("99.99"))
                .itemCount(2)
                .build();

        consumer.handleOrderCreated(event);

        verify(emailService).sendOrderConfirmation("user@test.com", "ORD-001", "99.99", 2);
    }

    @Test
    void handleOrderCreatedSkipsEmailWhenNoUserEmail() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderNumber("ORD-002")
                .userEmail(null)
                .build();

        consumer.handleOrderCreated(event);

        verifyNoInteractions(emailService);
    }

    @Test
    void handleOrderCreatedSkipsEmailWhenBlankEmail() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderNumber("ORD-003")
                .userEmail("")
                .build();

        consumer.handleOrderCreated(event);

        verifyNoInteractions(emailService);
    }

    @Test
    void handlePaymentCompletedSendsConfirmation() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderNumber("ORD-001")
                .userEmail("user@test.com")
                .transactionId("TXN-123")
                .amount(new BigDecimal("99.99"))
                .status("COMPLETED")
                .paymentMethod("CREDIT_CARD")
                .build();

        consumer.handlePaymentCompleted(event);

        verify(emailService).sendPaymentConfirmation("user@test.com", "ORD-001",
                "TXN-123", "99.99", "CREDIT_CARD");
    }

    @Test
    void handlePaymentCompletedSendsFailureEmail() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderNumber("ORD-001")
                .userEmail("user@test.com")
                .status("FAILED")
                .failureReason("Insufficient funds")
                .build();

        consumer.handlePaymentCompleted(event);

        verify(emailService).sendPaymentFailure("user@test.com", "ORD-001",
                "Insufficient funds");
    }
}
