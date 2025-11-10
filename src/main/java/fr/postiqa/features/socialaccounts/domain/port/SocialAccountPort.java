package fr.postiqa.features.socialaccounts.domain.port;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.shared.enums.SocialPlatform;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for social account persistence operations.
 * Defines the contract for managing social account data.
 */
public interface SocialAccountPort {

    /**
     * Save a social account.
     */
    SocialAccount save(SocialAccount account);

    /**
     * Find social account by ID.
     */
    Optional<SocialAccount> findById(UUID id);

    /**
     * Find social account by ID and organization ID (authorization check).
     */
    Optional<SocialAccount> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Find social account by ID and client ID (agency tenant isolation).
     */
    Optional<SocialAccount> findByIdAndClientId(UUID id, UUID clientId);

    /**
     * Find all social accounts for an organization.
     */
    List<SocialAccount> findByOrganizationId(UUID organizationId);

    /**
     * Find all active social accounts for an organization.
     */
    List<SocialAccount> findActiveByOrganizationId(UUID organizationId);

    /**
     * Find all social accounts for a client.
     */
    List<SocialAccount> findByClientId(UUID clientId);

    /**
     * Find all active social accounts for a client.
     */
    List<SocialAccount> findActiveByClientId(UUID clientId);

    /**
     * Find social account by organization, platform, and platform account ID.
     */
    Optional<SocialAccount> findByOrganizationAndPlatformAndPlatformAccountId(
        UUID organizationId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Find social account by client, platform, and platform account ID.
     */
    Optional<SocialAccount> findByClientAndPlatformAndPlatformAccountId(
        UUID clientId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Find all social accounts with tokens expiring before threshold.
     */
    List<SocialAccount> findAccountsWithExpiringTokens(Instant expirationThreshold);

    /**
     * Find all social accounts with expired tokens.
     */
    List<SocialAccount> findAccountsWithExpiredTokens(Instant now);

    /**
     * Delete a social account.
     */
    void delete(UUID id);

    /**
     * Check if a social account exists for organization, platform, and platform account ID.
     */
    boolean existsByOrganizationAndPlatformAndPlatformAccountId(
        UUID organizationId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Count social accounts for an organization.
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Count social accounts for a client.
     */
    long countByClientId(UUID clientId);
}
