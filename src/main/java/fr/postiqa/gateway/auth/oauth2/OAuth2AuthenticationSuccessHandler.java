package fr.postiqa.gateway.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.gateway.auth.usecase.HandleOAuth2LoginUseCase;
import fr.postiqa.shared.dto.auth.LoginResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;

/**
 * OAuth2 Authentication Success Handler.
 * Processes successful OAuth2 login and generates JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final HandleOAuth2LoginUseCase handleOAuth2LoginUseCase;
    private final ObjectMapper objectMapper;

    @Value("${oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Value("${oauth2.response-mode:redirect}") // redirect or json
    private String responseMode;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        log.info("OAuth2 authentication successful");

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        String provider = oauth2Token.getAuthorizedClientRegistrationId();

        // Extract OAuth2 tokens (if available)
        String accessToken = null; // OAuth2 access token from provider
        String refreshToken = null; // OAuth2 refresh token from provider
        Instant tokenExpiresAt = null;

        // Note: OAuth2 tokens are typically stored in OAuth2AuthorizedClient
        // For simplicity, we pass null here. In production, extract from OAuth2AuthorizedClientService

        // Process OAuth2 login and generate JWT
        LoginResponse loginResponse = handleOAuth2LoginUseCase.execute(
            oauth2User,
            provider,
            accessToken,
            refreshToken,
            tokenExpiresAt
        );

        // Return response based on mode
        if ("json".equalsIgnoreCase(responseMode)) {
            // Return JSON response (for mobile/API clients)
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getWriter(), loginResponse);
        } else {
            // Redirect to frontend with JWT token (for web clients)
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", loginResponse.getAccessToken())
                .queryParam("refresh_token", loginResponse.getRefreshToken())
                .queryParam("token_type", loginResponse.getTokenType())
                .queryParam("expires_in", loginResponse.getExpiresIn())
                .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }

        log.info("OAuth2 login completed for user: {}", loginResponse.getUser().getEmail());
    }
}
