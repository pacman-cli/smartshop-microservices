package com.smartshop.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing payment event for order: {} status: {}",
                event.getOrderNumber(), event.getStatus());
        kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, event.getOrderNumber(), event);
    }
}
