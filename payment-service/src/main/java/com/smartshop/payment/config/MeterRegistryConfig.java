package com.smartshop.payment.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom business metrics for Payment Service.
 * Tracks: payments processed, payments by status, payment processing time.
 */
@Configuration
public class MeterRegistryConfig {

    @Bean
    public Counter paymentsProcessedCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.payments.processed")
                .description("Total payments processed")
                .register(registry);
    }

    @Bean
    public Counter paymentsFailedCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.payments.failed")
                .description("Total failed payment attempts")
                .register(registry);
    }

    @Bean
    public Timer paymentProcessingTimer(MeterRegistry registry) {
        return Timer.builder("smartshop.payment.processing.time")
                .description("Time to process a payment")
                .register(registry);
    }

    @Bean
    public Counter paymentsRefundedCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.payments.refunded")
                .description("Total refunds issued")
                .register(registry);
    }
}