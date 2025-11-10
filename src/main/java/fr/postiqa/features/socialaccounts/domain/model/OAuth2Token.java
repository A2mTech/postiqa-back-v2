package fr.postiqa.features.socialaccounts.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Value object representing an OAuth2 access/refresh token pair.
 * Immutable representation of OAuth2 credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2Token {

    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private String tokenType;
    private String scope;

    /**
     * Check if the token is expired.
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the token is expiring soon (within specified seconds).
     */
    public boolean isExpiringSoon(long secondsThreshold) {
        if (expiresAt == null) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(secondsThreshold);
        return expiresAt.isBefore(threshold);
    }

    /**
     * Check if refresh token is available.
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }
}
