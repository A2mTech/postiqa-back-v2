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
 * OAuth2 strategy for Instagram (via Meta/Facebook Business API).
 * Documentation: https://developers.facebook.com/docs/instagram-basic-display-api
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstagramOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;

    @Value("${social.instagram.client-id}")
    private String clientId;

    @Value("${social.instagram.client-secret}")
    private String clientSecret;

    private static final String AUTH_URL = "https://api.instagram.com/oauth/authorize";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    private static final String LONG_LIVED_TOKEN_URL = "https://graph.instagram.com/access_token";
    private static final String REFRESH_TOKEN_URL = "https://graph.instagram.com/refresh_access_token";
    private static final String USER_INFO_URL = "https://graph.instagram.com/me";

    @Override
    public SocialPlatform getSupportedPlatform() {
        return SocialPlatform.INSTAGRAM;
    }

    @Override
    public OAuth2AuthorizationUrl generateAuthorizationUrl(String redirectUri, String state, String[] scopes) {
        String scopeString = String.join(",", scopes);

        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scopeString)
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .build()
            .toUriString();

        return OAuth2AuthorizationUrl.builder()
            .platform(SocialPlatform.INSTAGRAM)
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
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "authorization_code");
            body.add("redirect_uri", redirectUri);
            body.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from Instagram");
            }

            // Exchange short-lived token for long-lived token (60 days)
            String shortLivedToken = (String) response.get("access_token");
            return exchangeForLongLivedToken(shortLivedToken);

        } catch (Exception e) {
            log.error("Failed to exchange code for token on Instagram", e);
            throw new OAuth2AuthenticationException("Failed to obtain Instagram access token", e);
        }
    }

    @Override
    public OAuth2Token refreshToken(String refreshToken) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(REFRESH_TOKEN_URL)
                .queryParam("grant_type", "ig_refresh_token")
                .queryParam("access_token", refreshToken)
                .build()
                .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from Instagram");
            }

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to refresh Instagram token", e);
            throw new OAuth2AuthenticationException("Failed to refresh Instagram token", e);
        }
    }

    @Override
    public void revokeToken(String accessToken) {
        // Instagram doesn't have a revoke endpoint
        log.info("Instagram doesn't support token revocation - token will expire naturally");
    }

    @Override
    public Map<String, Object> fetchAccountInfo(String accessToken) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL)
                .queryParam("fields", "id,username,account_type")
                .queryParam("access_token", accessToken)
                .build()
                .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            return response;

        } catch (Exception e) {
            log.error("Failed to fetch Instagram account info", e);
            throw new OAuth2AuthenticationException("Failed to fetch Instagram account info", e);
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

    private OAuth2Token exchangeForLongLivedToken(String shortLivedToken) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(LONG_LIVED_TOKEN_URL)
                .queryParam("grant_type", "ig_exchange_token")
                .queryParam("client_secret", clientSecret)
                .queryParam("access_token", shortLivedToken)
                .build()
                .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from Instagram");
            }

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to exchange for long-lived Instagram token", e);
            throw new OAuth2AuthenticationException("Failed to get long-lived token", e);
        }
    }

    private OAuth2Token buildToken(Map<String, Object> response) {
        String accessToken = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");

        Instant expiresAt = expiresIn != null
            ? Instant.now().plusSeconds(expiresIn)
            : Instant.now().plusSeconds(5184000); // 60 days default for Instagram

        return OAuth2Token.builder()
            .accessToken(accessToken)
            .refreshToken(accessToken) // Instagram uses same token for refresh
            .expiresAt(expiresAt)
            .tokenType("Bearer")
            .scope("user_profile,user_media")
            .build();
    }
}
