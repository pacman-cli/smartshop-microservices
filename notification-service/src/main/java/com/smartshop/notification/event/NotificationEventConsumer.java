package com.smartshop.notification.event;

import com.smartshop.notification.dto.OrderCreatedEvent;
import com.smartshop.notification.dto.PaymentCompletedEvent;
import com.smartshop.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Event Consumer — listens for order and payment events
 * and triggers email notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailService emailService;

    @KafkaListener(
            topics = "order.created",
            groupId = "notification-group",
            containerFactory = "orderKafkaListenerContainerFactory")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event: {}", event.getOrderNumber());

        if (event.getUserEmail() != null && !event.getUserEmail().isBlank()) {
            emailService.sendOrderConfirmation(
                    event.getUserEmail(),
                    event.getOrderNumber(),
                    event.getTotalAmount().toPlainString(),
                    event.getItemCount());
        } else {
            log.warn("No email address for order: {}", event.getOrderNumber());
        }
    }

    @KafkaListener(
            topics = "payment.completed",
            groupId = "notification-group",
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment event: {} status={}", event.getOrderNumber(), event.getStatus());

        String userEmail = event.getUserEmail();
        if (userEmail == null || userEmail.isBlank()) {
            log.warn("No user email in payment event for order: {}, skipping notification",
                    event.getOrderNumber());
            return;
        }

        if ("COMPLETED".equals(event.getStatus())) {
            emailService.sendPaymentConfirmation(
                    userEmail,
                    event.getOrderNumber(),
                    event.getTransactionId(),
                    event.getAmount().toPlainString(),
                    event.getPaymentMethod());
        } else if ("FAILED".equals(event.getStatus())) {
            emailService.sendPaymentFailure(
                    userEmail,
                    event.getOrderNumber(),
                    event.getFailureReason() != null ? event.getFailureReason() : "Unknown error");
        }
    }
}
