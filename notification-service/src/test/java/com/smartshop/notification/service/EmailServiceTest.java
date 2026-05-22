package com.smartshop.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void sendOrderConfirmationSendsEmail() {
        emailService.sendOrderConfirmation("user@test.com", "ORD-001", "99.99", 3);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage msg = messageCaptor.getValue();

        assertThat(msg.getTo()).containsExactly("user@test.com");
        assertThat(msg.getSubject()).contains("ORD-001");
        assertThat(msg.getText()).contains("$99.99").contains("Items: 3")
                .contains("SmartShop");
    }

    @Test
    void sendPaymentConfirmationSendsEmail() {
        emailService.sendPaymentConfirmation("user@test.com", "ORD-001",
                "TXN-123", "99.99", "CREDIT_CARD");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage msg = messageCaptor.getValue();

        assertThat(msg.getTo()).containsExactly("user@test.com");
        assertThat(msg.getSubject()).contains("ORD-001");
        assertThat(msg.getText()).contains("TXN-123").contains("CREDIT_CARD");
    }

    @Test
    void sendPaymentFailureSendsEmail() {
        emailService.sendPaymentFailure("user@test.com", "ORD-001", "Insufficient funds");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage msg = messageCaptor.getValue();

        assertThat(msg.getTo()).containsExactly("user@test.com");
        assertThat(msg.getSubject()).contains("ORD-001");
        assertThat(msg.getText()).contains("Insufficient funds");
    }
}
