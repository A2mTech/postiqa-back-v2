package fr.postiqa.features.socialaccounts.infrastructure.strategy;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.shared.enums.SocialPlatform;
import fr.postiqa.shared.exception.socialaccount.OAuth2AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * OAuth2 strategy for LinkedIn.
 * Implements LinkedIn OAuth 2.0 authorization flow.
 * Documentation: https://learn.microsoft.com/en-us/linkedin/shared/authentication/authentication
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkedInOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;

    @Value("${social.linkedin.client-id}")
    private String clientId;

    @Value("${social.linkedin.client-secret}")
    private String clientSecret;

    private static final String AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String REVOKE_URL = "https://www.linkedin.com/oauth/v2/revoke";
    private static final String USER_INFO_URL = "https://api.linkedin.com/v2/userinfo";

    @Override
    public SocialPlatform getSupportedPlatform() {
        return SocialPlatform.LINKEDIN;
    }

    @Override
    public OAuth2AuthorizationUrl generateAuthorizationUrl(String redirectUri, String state, String[] scopes) {
        String scopeString = String.join(" ", scopes);

        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
            .queryParam("response_type", "code")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state)
            .queryParam("scope", scopeString)
            .build()
            .toUriString();

        return OAuth2AuthorizationUrl.builder()
            .platform(SocialPlatform.LINKEDIN)
            .url(url)
            .state(state)
            .redirectUri(redirectUri)
            .build();
    }

    @Override
    public OAuth2Token exchangeCodeForToken(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from LinkedIn");
            }

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to exchange code for token on LinkedIn", e);
            throw new OAuth2AuthenticationException("Failed to obtain LinkedIn access token", e);
        }
    }

    @Override
    public OAuth2Token refreshToken(String refreshToken) {
        // LinkedIn does not support refresh tokens by default
        throw new UnsupportedOperationException("LinkedIn does not support token refresh");
    }

    @Override
    public void revokeToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", accessToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(REVOKE_URL, request, String.class);

            log.info("Successfully revoked LinkedIn token");

        } catch (Exception e) {
            log.warn("Failed to revoke LinkedIn token", e);
        }
    }

    @Override
    public Map<String, Object> fetchAccountInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                USER_INFO_URL,
                HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            return response;

        } catch (Exception e) {
            log.error("Failed to fetch LinkedIn account info", e);
            throw new OAuth2AuthenticationException("Failed to fetch LinkedIn account info", e);
        }
    }

    @Override
    public boolean testConnection(String accessToken) {
        try {
            fetchAccountInfo(accessToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private OAuth2Token buildToken(Map<String, Object> response) {
        String accessToken = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");
        String scope = (String) response.get("scope");

        Instant expiresAt = expiresIn != null
            ? Instant.now().plusSeconds(expiresIn)
            : null;

        return OAuth2Token.builder()
            .accessToken(accessToken)
            .refreshToken(null) // LinkedIn doesn't provide refresh tokens
            .expiresAt(expiresAt)
            .tokenType("Bearer")
            .scope(scope)
            .build();
    }
}
