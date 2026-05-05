package com.smartshop.payment.config;

import com.smartshop.contracts.security.GatewayHeaderAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for payment-service.
 *
 * - All payment endpoints require authentication
 * - Refund requires ADMIN role
 * - Actuator endpoints are public
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GatewayHeaderAuthFilter gatewayHeaderAuthFilter() {
        return new GatewayHeaderAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        // Refund requires admin
                        .requestMatchers("/api/payments/*/refund").hasRole("ADMIN")
                        // All other payment endpoints require authentication
                        .requestMatchers("/api/payments/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(gatewayHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
