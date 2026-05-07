package com.smartshop.order.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for communicating with User Service.
 * Uses Eureka service discovery (name = "user-service").
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Bulkhead(name = "userService")
    UserResponse getUserById(@PathVariable("id") Long id);

    default UserResponse getUserFallback(Long id, Throwable t) {
        // Return a degraded user response
        return UserResponse.builder()
                .id(id)
                .name("User profile unavailable")
                .email("unavailable@smartshop.com")
                .role("CUSTOMER")
                .build();
    }
}
