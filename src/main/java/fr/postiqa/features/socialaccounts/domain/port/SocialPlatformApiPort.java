package fr.postiqa.features.socialaccounts.domain.port;

import fr.postiqa.features.socialaccounts.domain.model.ConnectionTestResult;
import fr.postiqa.shared.enums.SocialPlatform;

/**
 * Port for testing social platform API connections.
 * Defines the contract for validating account connections.
 */
public interface SocialPlatformApiPort {

    /**
     * Test if the connection to a social platform is valid.
     *
     * @param platform The social media platform
     * @param accessToken Access token to test
     * @return Connection test result
     */
    ConnectionTestResult testConnection(
        SocialPlatform platform,
        String accessToken
    );

    /**
     * Verify if access token is still valid.
     *
     * @param platform The social media platform
     * @param accessToken Access token to verify
     * @return True if token is valid, false otherwise
     */
    boolean isTokenValid(
        SocialPlatform platform,
        String accessToken
    );
}
