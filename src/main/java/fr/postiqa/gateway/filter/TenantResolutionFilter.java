package fr.postiqa.gateway.filter;

import fr.postiqa.gateway.auth.authorization.TenantContextHolder;
import fr.postiqa.gateway.auth.jwt.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Tenant Resolution Filter.
 * Extracts tenant context (user_id, organization_id, client_id) from JWT authentication
 * and stores it in ThreadLocal for current request.
 * Must run AFTER JwtAuthenticationFilter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantResolutionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Extract tenant context from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                // Extract IDs from JWT claims
                UUID userId = jwtAuth.getUserId();
                UUID organizationId = jwtAuth.getOrganizationId();
                UUID clientId = jwtAuth.getClientId();

                // Store in ThreadLocal
                if (userId != null) {
                    TenantContextHolder.setUserId(userId);
                }
                if (organizationId != null) {
                    TenantContextHolder.setOrganizationId(organizationId);
                }
                if (clientId != null) {
                    TenantContextHolder.setClientId(clientId);
                }

                log.debug("Resolved tenant context - User: {}, Org: {}, Client: {}",
                    userId, organizationId, clientId);
            }
        } catch (Exception e) {
            log.warn("Failed to resolve tenant context: {}", e.getMessage());
            // Continue without tenant context
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear ThreadLocal to prevent memory leaks
            TenantContextHolder.clear();
        }
    }
}
