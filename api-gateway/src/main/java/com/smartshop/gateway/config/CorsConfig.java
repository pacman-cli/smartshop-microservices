package com.smartshop.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS Configuration for the API Gateway.
 *
 * Why here?
 * Since ALL requests enter through the gateway, we configure CORS once here
 * instead of repeating it in every microservice.
 *
 * What is CORS?
 * When a frontend app at http://localhost:3000 calls our API at http://localhost:8080,
 * the browser blocks it by default (different origin). CORS headers tell the browser
 * "it's okay, allow this request."
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allow requests from any origin (restrict in production)
        corsConfig.setAllowedOrigins(List.of("*"));

        // Allow these HTTP methods
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow these headers in requests
        corsConfig.setAllowedHeaders(List.of("*"));

        // Apply CORS config to all routes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
