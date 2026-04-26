package com.smartshop.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting configuration for the API Gateway.
 *
 * Uses Redis-backed token bucket algorithm via Spring Cloud Gateway.
 * Rate limits are applied per IP address by default.
 *
 * Configuration (in application.properties or config-server):
 * - replenishRate: tokens added per second
 * - burstCapacity: max tokens in bucket (handles bursts)
 * - requestedTokens: tokens consumed per request
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Key resolver that rate-limits by client IP address.
     * Falls back to "anonymous" if IP cannot be determined.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just(ip);
        };
    }
}
