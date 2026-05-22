package com.smartshop.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterRegistryConfig {

    @Bean
    public Counter ordersCreatedCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.orders.created")
                .description("Total orders created")
                .register(registry);
    }

    @Bean
    public Timer orderProcessingTimer(MeterRegistry registry) {
        return Timer.builder("smartshop.order.processing.time")
                .description("Time to process an order")
                .register(registry);
    }

    @Bean
    public Counter ordersFailedCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.orders.failed")
                .description("Total failed order attempts")
                .register(registry);
    }

    @Bean
    public TaggedCircuitBreakerMetrics taggedCircuitBreakerMetrics(
            CircuitBreakerRegistry circuitBreakerRegistry, MeterRegistry meterRegistry) {
        TaggedCircuitBreakerMetrics metrics = TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(circuitBreakerRegistry);
        metrics.bindTo(meterRegistry);
        return metrics;
    }
}