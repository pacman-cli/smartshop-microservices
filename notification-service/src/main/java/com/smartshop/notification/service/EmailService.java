package com.smartshop.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email Service — sends email notifications.
 * Uses Spring Boot Mail Starter with a configurable SMTP server.
 * In development, use MailHog (localhost:1025) to capture emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOrderConfirmation(String to, String orderNumber, String totalAmount, int itemCount) {
        String subject = "SmartShop - Order Confirmed: " + orderNumber;
        String body = String.format(
                "Hello!\n\n" +
                "Your order %s has been placed successfully.\n\n" +
                "Order Details:\n" +
                "- Order Number: %s\n" +
                "- Items: %d\n" +
                "- Total Amount: $%s\n\n" +
                "We'll notify you when your payment is processed.\n\n" +
                "Thank you for shopping with SmartShop!",
                orderNumber, orderNumber, itemCount, totalAmount);

        sendEmail(to, subject, body);
    }

    public void sendPaymentConfirmation(String to, String orderNumber, String transactionId,
                                         String amount, String paymentMethod) {
        String subject = "SmartShop - Payment Confirmed: " + orderNumber;
        String body = String.format(
                "Hello!\n\n" +
                "Your payment has been processed successfully.\n\n" +
                "Payment Details:\n" +
                "- Order Number: %s\n" +
                "- Transaction ID: %s\n" +
                "- Amount: $%s\n" +
                "- Payment Method: %s\n\n" +
                "Your order is now being prepared for shipping.\n\n" +
                "Thank you for shopping with SmartShop!",
                orderNumber, transactionId, amount, paymentMethod);

        sendEmail(to, subject, body);
    }

    public void sendPaymentFailure(String to, String orderNumber, String reason) {
        String subject = "SmartShop - Payment Failed: " + orderNumber;
        String body = String.format(
                "Hello!\n\n" +
                "Unfortunately, your payment for order %s has failed.\n\n" +
                "Reason: %s\n\n" +
                "Please try again or use a different payment method.\n\n" +
                "If you need help, contact our support team.\n\n" +
                "SmartShop Team",
                orderNumber, reason);

        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@smartshop.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent to {} - subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} - subject: {}", to, subject, e);
        }
    }
}
