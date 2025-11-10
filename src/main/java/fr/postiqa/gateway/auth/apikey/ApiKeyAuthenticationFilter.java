package fr.postiqa.gateway.auth.apikey;

import fr.postiqa.database.entity.ApiKeyEntity;
import fr.postiqa.gateway.auth.CustomUserDetailsService;
import fr.postiqa.gateway.auth.usecase.ValidateApiKeyUseCase;
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

/**
 * API Key Authentication Filter.
 * Extracts API key from X-API-Key header and validates it.
 * Sets authentication in Spring Security context if valid.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ValidateApiKeyUseCase validateApiKeyUseCase;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String apiKey = extractApiKeyFromRequest(request);

            if (apiKey != null) {
                // Validate API key
                ApiKeyEntity apiKeyEntity = validateApiKeyUseCase.execute(apiKey);

                // Load user details
                String email = apiKeyEntity.getUser().getEmail();
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // Create authentication token
                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                    userDetails,
                    apiKey,
                    userDetails.getAuthorities(),
                    apiKeyEntity.getUser().getId(),
                    apiKeyEntity.getId()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set API key authentication for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Cannot set API key authentication: {}", e.getMessage());
            // Don't set authentication, let the request continue
            // The SecurityConfig will handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract API key from X-API-Key header
     */
    private String extractApiKeyFromRequest(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        return null;
    }
}
