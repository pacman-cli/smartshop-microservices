package com.smartshop.product.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads X-User-Email and X-User-Role headers set by the API Gateway
 * after JWT validation, and creates a Spring Security authentication context.
 *
 * This allows downstream services to use @PreAuthorize, hasRole(), etc.
 * without needing to validate JWT themselves.
 */
@Component
@Slf4j
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");

        if (userEmail != null && !userEmail.isBlank() && userRole != null && !userRole.isBlank()) {
            // Spring Security expects roles prefixed with "ROLE_"
            String roleWithPrefix = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            List.of(new SimpleGrantedAuthority(roleWithPrefix)));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Gateway auth: user={} role={}", userEmail, userRole);
        }

        filterChain.doFilter(request, response);
    }
}
