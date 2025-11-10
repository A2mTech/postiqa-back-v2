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
 * OAuth2 strategy for YouTube (via Google API).
 * Documentation: https://developers.google.com/youtube/v3/guides/auth/server-side-web-apps
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YouTubeOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;

    @Value("${social.youtube.client-id}")
    private String clientId;

    @Value("${social.youtube.client-secret}")
    private String clientSecret;

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String USER_INFO_URL = "https://www.googleapis.com/youtube/v3/channels";

    @Override
    public SocialPlatform getSupportedPlatform() {
        return SocialPlatform.YOUTUBE;
    }

    @Override
    public OAuth2AuthorizationUrl generateAuthorizationUrl(String redirectUri, String state, String[] scopes) {
        String scopeString = String.join(" ", scopes);

        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", scopeString)
            .queryParam("state", state)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .build()
            .toUriString();

        return OAuth2AuthorizationUrl.builder()
            .platform(SocialPlatform.YOUTUBE)
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
            body.add("code", code);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("redirect_uri", redirectUri);
            body.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from YouTube");
            }

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to exchange code for token on YouTube", e);
            throw new OAuth2AuthenticationException("Failed to obtain YouTube access token", e);
        }
    }

    @Override
    public OAuth2Token refreshToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);
            body.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from YouTube");
            }

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to refresh YouTube token", e);
            throw new OAuth2AuthenticationException("Failed to refresh YouTube token", e);
        }
    }

    @Override
    public void revokeToken(String accessToken) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(REVOKE_URL)
                .queryParam("token", accessToken)
                .build()
                .toUriString();

            restTemplate.postForObject(url, null, String.class);

            log.info("Successfully revoked YouTube token");

        } catch (Exception e) {
            log.warn("Failed to revoke YouTube token", e);
        }
    }

    @Override
    public Map<String, Object> fetchAccountInfo(String accessToken) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL)
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("mine", "true")
                .build()
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

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
            log.error("Failed to fetch YouTube channel info", e);
            throw new OAuth2AuthenticationException("Failed to fetch YouTube channel info", e);
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
        String refreshToken = (String) response.get("refresh_token");
        Integer expiresIn = (Integer) response.get("expires_in");
        String scope = (String) response.get("scope");

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
