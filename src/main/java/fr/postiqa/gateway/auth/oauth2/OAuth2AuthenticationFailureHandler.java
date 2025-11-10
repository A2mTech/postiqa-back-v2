package fr.postiqa.gateway.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Authentication Failure Handler.
 * Handles failed OAuth2 authentication attempts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Value("${oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Value("${oauth2.response-mode:redirect}") // redirect or json
    private String responseMode;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        String errorMessage = exception.getMessage();

        // Return response based on mode
        if ("json".equalsIgnoreCase(responseMode)) {
            // Return JSON error response (for mobile/API clients)
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "oauth2_authentication_failed");
            errorResponse.put("message", errorMessage);
            errorResponse.put("timestamp", System.currentTimeMillis());

            objectMapper.writeValue(response.getWriter(), errorResponse);
        } else {
            // Redirect to frontend with error (for web clients)
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", "oauth2_failed")
                .queryParam("message", errorMessage)
                .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
