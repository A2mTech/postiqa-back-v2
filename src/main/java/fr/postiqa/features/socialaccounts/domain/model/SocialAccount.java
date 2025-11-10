package fr.postiqa.features.socialaccounts.domain.model;

import fr.postiqa.shared.enums.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain model for SocialAccount.
 * Represents a connected social media account in the business domain.
 * NO infrastructure dependencies - pure business logic only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccount {

    private UUID id;
    private UUID userId;
    private UUID organizationId;
    private UUID clientId;
    private SocialPlatform platform;
    private String platformAccountId;
    private String accountName;
    private String accountHandle;
    private String accountAvatarUrl;
    private OAuth2Token token;
    private String scopes;
    private Map<String, Object> platformMetadata;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Check if the token is expired.
     */
    public boolean isTokenExpired() {
        if (token == null || token.getExpiresAt() == null) {
            return false;
        }
        return Instant.now().isAfter(token.getExpiresAt());
    }

    /**
     * Check if the token is expiring soon (within 1 hour).
     */
    public boolean isTokenExpiringSoon() {
        if (token == null || token.getExpiresAt() == null) {
            return false;
        }
        Instant oneHourFromNow = Instant.now().plusSeconds(3600);
        return token.getExpiresAt().isBefore(oneHourFromNow);
    }

    /**
     * Update the token.
     */
    public void updateToken(OAuth2Token newToken) {
        this.token = newToken;
        this.updatedAt = Instant.now();
    }

    /**
     * Deactivate the account.
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Activate the account.
     */
    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }
}
