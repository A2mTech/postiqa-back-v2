package fr.postiqa.shared.dto.socialaccount;

import fr.postiqa.shared.enums.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for SocialAccount entity.
 * Represents a connected social media account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccountDto {

    private UUID id;
    private UUID userId;
    private UUID organizationId;
    private UUID clientId;
    private SocialPlatform platform;
    private String platformAccountId;
    private String accountName;
    private String accountHandle;
    private String accountAvatarUrl;
    private Instant tokenExpiresAt;
    private String scopes;
    private Map<String, Object> platformMetadata;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Computed fields
    private Boolean tokenExpired;
    private Boolean tokenExpiringSoon;
}
