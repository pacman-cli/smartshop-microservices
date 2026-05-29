package com.smartshop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.contracts.event.OrderCreatedEvent;
import com.smartshop.order.entity.OutboxEvent;
import com.smartshop.order.event.OrderEventProducer;
import com.smartshop.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Publisher — background task that reliably sends events to Kafka.
 *
 * Polling Strategy:
 * 1. Find PENDING events in the outbox table.
 * 2. For each event, attempt to publish to Kafka.
 * 3. On success: Mark as PROCESSED.
 * 4. On failure: Increment retry count and log error.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events in outbox", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() > 10) {
                    event.setStatus("FAILED");
                }
            }
        }
        
        outboxRepository.saveAll(pendingEvents);
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        if ("ORDER_CREATED".equals(event.getEventType())) {
            OrderCreatedEvent orderEvent = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
            orderEventProducer.publishOrderCreated(orderEvent);
            log.info("Successfully published ORDER_CREATED event for order: {}", event.getAggregateId());
        } else {
            log.warn("Unknown event type in outbox: {}", event.getEventType());
            event.setStatus("SKIPPED");
        }
    }
}
