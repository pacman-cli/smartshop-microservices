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
        String userId = request.getHeader("X-User-Id");

        if (userEmail != null && !userEmail.isBlank() && userRole != null && !userRole.isBlank()) {
            if (userId == null || userId.isBlank()) {
                log.warn("Gateway headers present but X-User-Id is missing. Skipping authentication.");
            } else {
                // Spring Security expects roles prefixed with "ROLE_"
                String roleWithPrefix = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

                // Store UserID as principal for easier access in @AuthenticationPrincipal or SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority(roleWithPrefix)));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Gateway auth successful: id={} email={} role={}", userId, userEmail, userRole);
            }
        }

        filterChain.doFilter(request, response);
    }
}
