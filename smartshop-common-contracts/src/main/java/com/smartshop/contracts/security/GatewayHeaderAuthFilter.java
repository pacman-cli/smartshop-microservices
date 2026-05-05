package com.smartshop.contracts.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads {@code X-User-Email} and {@code X-User-Role} headers set by the API Gateway
 * after JWT validation, and creates a Spring Security authentication context.
 *
 * <p>This allows downstream services to use {@code @PreAuthorize}, {@code hasRole()}, etc.
 * without needing to validate JWT themselves.</p>
 *
 * <p><b>Note:</b> This class is intentionally NOT annotated with {@code @Component}.
 * Each service should register it as a bean in their own SecurityConfig to maintain
 * explicit control over the filter chain.</p>
 */
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
