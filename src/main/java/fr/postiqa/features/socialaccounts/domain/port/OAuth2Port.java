package fr.postiqa.features.socialaccounts.domain.port;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.shared.enums.SocialPlatform;

import java.util.Map;

/**
 * Port for OAuth2 operations.
 * Defines the contract for OAuth2 authentication and token management.
 */
public interface OAuth2Port {

    /**
     * Generate OAuth2 authorization URL for a platform.
     *
     * @param platform The social media platform
     * @param redirectUri The redirect URI after authorization
     * @param state State parameter for CSRF protection
     * @param scopes Requested OAuth2 scopes
     * @return OAuth2 authorization URL
     */
    OAuth2AuthorizationUrl generateAuthorizationUrl(
        SocialPlatform platform,
        String redirectUri,
        String state,
        String[] scopes
    );

    /**
     * Exchange authorization code for access token.
     *
     * @param platform The social media platform
     * @param code Authorization code from OAuth2 callback
     * @param redirectUri The redirect URI used during authorization
     * @return OAuth2 token with access and refresh tokens
     */
    OAuth2Token exchangeCodeForToken(
        SocialPlatform platform,
        String code,
        String redirectUri
    );

    /**
     * Refresh an expired access token using refresh token.
     *
     * @param platform The social media platform
     * @param refreshToken Refresh token
     * @return New OAuth2 token
     */
    OAuth2Token refreshToken(
        SocialPlatform platform,
        String refreshToken
    );

    /**
     * Revoke OAuth2 token (disconnect account).
     *
     * @param platform The social media platform
     * @param accessToken Access token to revoke
     */
    void revokeToken(
        SocialPlatform platform,
        String accessToken
    );

    /**
     * Fetch account information from platform using access token.
     *
     * @param platform The social media platform
     * @param accessToken Access token
     * @return Account information (id, name, handle, etc.)
     */
    Map<String, Object> fetchAccountInfo(
        SocialPlatform platform,
        String accessToken
    );
}
