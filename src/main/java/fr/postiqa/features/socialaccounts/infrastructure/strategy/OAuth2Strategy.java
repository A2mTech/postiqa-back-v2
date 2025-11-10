package fr.postiqa.features.socialaccounts.infrastructure.strategy;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.shared.enums.SocialPlatform;

import java.util.Map;

/**
 * Strategy interface for platform-specific OAuth2 operations.
 * Each social media platform implements this interface with its specific OAuth2 flow.
 */
public interface OAuth2Strategy {

    /**
     * Get the social platform this strategy supports.
     */
    SocialPlatform getSupportedPlatform();

    /**
     * Generate OAuth2 authorization URL.
     */
    OAuth2AuthorizationUrl generateAuthorizationUrl(String redirectUri, String state, String[] scopes);

    /**
     * Exchange authorization code for access token.
     */
    OAuth2Token exchangeCodeForToken(String code, String redirectUri);

    /**
     * Refresh an expired access token.
     */
    OAuth2Token refreshToken(String refreshToken);

    /**
     * Revoke an access token.
     */
    void revokeToken(String accessToken);

    /**
     * Fetch account information from platform.
     */
    Map<String, Object> fetchAccountInfo(String accessToken);

    /**
     * Test if the connection is valid.
     */
    boolean testConnection(String accessToken);
}
