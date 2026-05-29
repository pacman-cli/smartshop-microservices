package com.smartshop.order.event;

import com.smartshop.contracts.event.OrderCreatedEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String ORDER_CREATED_TOPIC = "order.created";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing order created event: {}", event.getOrderNumber());
        kafkaTemplate.send(ORDER_CREATED_TOPIC, event.getOrderNumber(), event);
    }

    @PreDestroy
    public void flush() {
        kafkaTemplate.flush();
        log.info("Order event producer flushed");
    }
}
