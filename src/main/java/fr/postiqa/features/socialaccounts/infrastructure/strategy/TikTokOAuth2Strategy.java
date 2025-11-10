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
 * OAuth2 strategy for TikTok.
 * Documentation: https://developers.tiktok.com/doc/login-kit-web
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TikTokOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;

    @Value("${social.tiktok.client-id}")
    private String clientId;

    @Value("${social.tiktok.client-secret}")
    private String clientSecret;

    private static final String AUTH_URL = "https://www.tiktok.com/v2/auth/authorize";
    private static final String TOKEN_URL = "https://open.tiktokapis.com/v2/oauth/token/";
    private static final String REFRESH_TOKEN_URL = "https://open.tiktokapis.com/v2/oauth/token/";
    private static final String REVOKE_URL = "https://open.tiktokapis.com/v2/oauth/revoke/";
    private static final String USER_INFO_URL = "https://open.tiktokapis.com/v2/user/info/";

    @Override
    public SocialPlatform getSupportedPlatform() {
        return SocialPlatform.TIKTOK;
    }

    @Override
    public OAuth2AuthorizationUrl generateAuthorizationUrl(String redirectUri, String state, String[] scopes) {
        String scopeString = String.join(",", scopes);

        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
            .queryParam("client_key", clientId)
            .queryParam("scope", scopeString)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state)
            .build()
            .toUriString();

        return OAuth2AuthorizationUrl.builder()
            .platform(SocialPlatform.TIKTOK)
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
            body.add("client_key", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("grant_type", "authorization_code");
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("data")) {
                throw new OAuth2AuthenticationException("Invalid response from TikTok");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return buildToken(data);

        } catch (Exception e) {
            log.error("Failed to exchange code for token on TikTok", e);
            throw new OAuth2AuthenticationException("Failed to obtain TikTok access token", e);
        }
    }

    @Override
    public OAuth2Token refreshToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_key", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(REFRESH_TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("data")) {
                throw new OAuth2AuthenticationException("Invalid response from TikTok");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return buildToken(data);

        } catch (Exception e) {
            log.error("Failed to refresh TikTok token", e);
            throw new OAuth2AuthenticationException("Failed to refresh TikTok token", e);
        }
    }

    @Override
    public void revokeToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_key", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", accessToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(REVOKE_URL, request, String.class);

            log.info("Successfully revoked TikTok token");

        } catch (Exception e) {
            log.warn("Failed to revoke TikTok token", e);
        }
    }

    @Override
    public Map<String, Object> fetchAccountInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            String url = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL)
                .queryParam("fields", "open_id,union_id,avatar_url,display_name")
                .build()
                .toUriString();

            HttpEntity<?> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            return response;

        } catch (Exception e) {
            log.error("Failed to fetch TikTok user info", e);
            throw new OAuth2AuthenticationException("Failed to fetch TikTok user info", e);
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

    private OAuth2Token buildToken(Map<String, Object> data) {
        String accessToken = (String) data.get("access_token");
        String refreshToken = (String) data.get("refresh_token");
        Integer expiresIn = (Integer) data.get("expires_in");
        String scope = (String) data.get("scope");

        Instant expiresAt = expiresIn != null
            ? Instant.now().plusSeconds(expiresIn)
            : null;

        return OAuth2Token.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresAt(expiresAt)
            .tokenType("Bearer")
            .scope(scope)
            .build();
    }
}
