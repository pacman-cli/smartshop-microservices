package com.smartshop.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic Route Configuration (Alternative to properties-based routes).
 *
 * This class shows how to define routes using Java code.
 * We already defined routes in application.properties — this is an
 * ALTERNATIVE approach. In production, teams often use one or the other.
 *
 * Currently DISABLED (commented out) because we're using properties-based config.
 * Uncomment the @Bean if you prefer Java-based route definitions.
 *
 * Why show both approaches?
 * - Properties: simpler, easier to change without recompiling
 * - Java code: more powerful, supports custom filters and logic
 */
@Configuration
@Slf4j
public class GatewayRoutesConfig {

    // Uncomment this method to use Java-based routes instead of properties-based.
    // If you enable this, REMOVE the route definitions from application.properties
    // to avoid duplicate routes.

    // @Bean
    public RouteLocator smartShopRoutes(RouteLocatorBuilder builder) {
        log.info("Initializing SmartShop Gateway Routes (Java config)");

        return builder.routes()
                // Route to User Service
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/auth/**")
                        .uri("lb://USER-SERVICE"))

                // Route to Product Service
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .uri("lb://PRODUCT-SERVICE"))

                // Route to Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .uri("lb://ORDER-SERVICE"))

                // Route to Payment Service
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .uri("lb://PAYMENT-SERVICE"))

                .build();
    }
}
