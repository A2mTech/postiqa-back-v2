package fr.postiqa.gateway.auth.jwt;

import fr.postiqa.gateway.auth.CustomUserDetailsService;
import fr.postiqa.shared.exception.auth.InvalidTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter.
 * Extracts JWT from request header and validates it.
 * Sets authentication in Spring Security context if valid.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                // Check token type
                String tokenType = jwtTokenProvider.getTokenType(jwt);
                if (!"access".equals(tokenType)) {
                    throw new InvalidTokenException("Invalid token type. Expected access token.");
                }

                String email = jwtTokenProvider.getEmailFromToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // Extract claims for scopes (organization_id, client_id)
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);
                UUID userId = claims.get("user_id") != null ? UUID.fromString((String) claims.get("user_id")) : null;
                UUID organizationId = claims.get("organization_id") != null ? UUID.fromString((String) claims.get("organization_id")) : null;
                UUID clientId = claims.get("client_id") != null ? UUID.fromString((String) claims.get("client_id")) : null;

                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    userDetails,
                    jwt,
                    userDetails.getAuthorities(),
                    userId,
                    organizationId,
                    clientId,
                    claims
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Don't set authentication, let the request continue
            // The SecurityConfig will handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
