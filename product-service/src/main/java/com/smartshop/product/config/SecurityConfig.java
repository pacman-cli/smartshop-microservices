package com.smartshop.product.config;

import com.smartshop.product.security.GatewayHeaderAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for product-service.
 *
 * The API Gateway validates JWT tokens and forwards user info
 * via X-User-Email and X-User-Role headers. This config reads
 * those headers and enforces access control.
 *
 * - GET /api/products/** is public (browsing)
 * - POST/PUT/DELETE /api/products/** require ADMIN role
 * - Stock reduction/restoration endpoints are internal (service-to-service)
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Health/actuator endpoints are public
                        .requestMatchers("/actuator/**").permitAll()
                        // GET on products is public (browsing catalog)
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // Stock endpoints are internal (service-to-service via Feign)
                        .requestMatchers(HttpMethod.PUT, "/api/products/*/reduce-stock").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/products/*/restore-stock").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/batch-reduce-stock").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/batch-restore-stock").permitAll()
                        // Product CRUD (create, update, delete) requires ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
