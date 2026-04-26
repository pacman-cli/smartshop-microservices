package com.smartshop.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT Authentication Filter for the API Gateway.
 *
 * This filter:
 * 1. Skips public endpoints (auth, actuator, eureka)
 * 2. Validates JWT tokens on protected routes
 * 3. Passes user info (email, role) as headers to downstream services
 */
@Component
@Slf4j
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;

    /** Paths that don't require authentication */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/actuator/",
            "/eureka",
            "/api/products"  // Product browsing is public
    );

    /** Paths where GET is allowed without auth but other methods require auth */
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/products",
            "/api/users"
    );

    public JwtAuthGatewayFilter(@Value("${jwt.secret:smartshop-secret-key-for-jwt-token-generation-min-256-bits-long-key}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Skip auth for fully public paths
        if (isFullyPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Allow GET on public-GET paths without auth
        if ("GET".equals(method) && isPublicGetPath(path)) {
            return chain.filter(exchange);
        }

        // All other requests require a valid JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // Pass user info to downstream services via headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }

    private boolean isFullyPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isPublicGetPath(String path) {
        return PUBLIC_GET_PATHS.stream().anyMatch(path::startsWith);
    }
}
