package com.smartshop.user.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom business metrics for User Service.
 * Tracks: user registrations, logins, authentication failures.
 */
@Configuration
public class MeterRegistryConfig {

    @Bean
    public Counter userRegistrationsCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.users.registered")
                .description("Total user registrations")
                .register(registry);
    }

    @Bean
    public Counter userLoginsCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.users.logins")
                .description("Total successful user logins")
                .register(registry);
    }

    @Bean
    public Counter authFailuresCounter(MeterRegistry registry) {
        return Counter.builder("smartshop.auth.failures")
                .description("Total authentication failures")
                .register(registry);
    }

    @Bean
    public Timer loginTimer(MeterRegistry registry) {
        return Timer.builder("smartshop.login.time")
                .description("Time for user login")
                .register(registry);
    }
}