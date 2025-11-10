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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 strategy for Twitter/X.
 * Implements Twitter OAuth 2.0 with PKCE flow.
 * Documentation: https://developer.twitter.com/en/docs/authentication/oauth-2-0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TwitterOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;

    @Value("${social.twitter.client-id}")
    private String clientId;

    @Value("${social.twitter.client-secret}")
    private String clientSecret;

    private static final String AUTH_URL = "https://twitter.com/i/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.twitter.com/2/oauth2/token";
    private static final String REVOKE_URL = "https://api.twitter.com/2/oauth2/revoke";
    private static final String USER_INFO_URL = "https://api.twitter.com/2/users/me";

    // Store code_verifier temporarily (in production, use Redis or database)
    private final Map<String, String> codeVerifierStore = new ConcurrentHashMap<>();

    @Override
    public SocialPlatform getSupportedPlatform() {
        return SocialPlatform.TWITTER;
    }

    @Override
    public OAuth2AuthorizationUrl generateAuthorizationUrl(String redirectUri, String state, String[] scopes) {
        String scopeString = String.join(" ", scopes);

        // Generate PKCE code_verifier and code_challenge
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // Store code_verifier for later use
        codeVerifierStore.put(state, codeVerifier);

        String url = UriComponentsBuilder.fromHttpUrl(AUTH_URL)
            .queryParam("response_type", "code")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scopeString)
            .queryParam("state", state)
            .queryParam("code_challenge", codeChallenge)
            .queryParam("code_challenge_method", "S256")
            .build()
            .toUriString();

        return OAuth2AuthorizationUrl.builder()
            .platform(SocialPlatform.TWITTER)
            .url(url)
            .state(state)
            .redirectUri(redirectUri)
            .build();
    }

    @Override
    public OAuth2Token exchangeCodeForToken(String code, String redirectUri) {
        try {
            // Retrieve code_verifier (in production, retrieve from Redis/DB by state)
            String codeVerifier = codeVerifierStore.values().stream().findFirst()
                .orElseThrow(() -> new OAuth2AuthenticationException("Code verifier not found"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("code_verifier", codeVerifier);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from Twitter");
            }

            // Clean up code_verifier
            codeVerifierStore.clear();

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to exchange code for token on Twitter", e);
            throw new OAuth2AuthenticationException("Failed to obtain Twitter access token", e);
        }
    }

    @Override
    public OAuth2Token refreshToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2AuthenticationException("Invalid response from Twitter");
            }

            return buildToken(response);

        } catch (Exception e) {
            log.error("Failed to refresh Twitter token", e);
            throw new OAuth2AuthenticationException("Failed to refresh Twitter token", e);
        }
    }

    @Override
    public void revokeToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", accessToken);
            body.add("token_type_hint", "access_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(REVOKE_URL, request, String.class);

            log.info("Successfully revoked Twitter token");

        } catch (Exception e) {
            log.warn("Failed to revoke Twitter token", e);
        }
    }

    @Override
    public Map<String, Object> fetchAccountInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = USER_INFO_URL + "?user.fields=id,name,username,profile_image_url";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            return response;

        } catch (Exception e) {
            log.error("Failed to fetch Twitter account info", e);
            throw new OAuth2AuthenticationException("Failed to fetch Twitter account info", e);
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

    private String generateCodeVerifier() {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }
}
