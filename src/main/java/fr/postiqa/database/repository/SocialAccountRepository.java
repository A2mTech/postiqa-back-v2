package fr.postiqa.database.repository;

import fr.postiqa.database.entity.ClientEntity;
import fr.postiqa.database.entity.OrganizationEntity;
import fr.postiqa.database.entity.SocialAccountEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.shared.enums.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SocialAccountEntity.
 * Provides data access methods for social account management.
 */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, UUID> {

    /**
     * Find all social accounts for a specific organization
     */
    List<SocialAccountEntity> findByOrganization(OrganizationEntity organization);

    /**
     * Find all social accounts for a specific organization by ID
     */
    List<SocialAccountEntity> findByOrganizationId(UUID organizationId);

    /**
     * Find all active social accounts for a specific organization
     */
    List<SocialAccountEntity> findByOrganizationIdAndActiveTrue(UUID organizationId);

    /**
     * Find all social accounts for a specific client
     */
    List<SocialAccountEntity> findByClient(ClientEntity client);

    /**
     * Find all social accounts for a specific client by ID
     */
    List<SocialAccountEntity> findByClientId(UUID clientId);

    /**
     * Find all active social accounts for a specific client
     */
    List<SocialAccountEntity> findByClientIdAndActiveTrue(UUID clientId);

    /**
     * Find all social accounts for a specific user
     */
    List<SocialAccountEntity> findByUser(UserEntity user);

    /**
     * Find all social accounts for a specific user by ID
     */
    List<SocialAccountEntity> findByUserId(UUID userId);

    /**
     * Find social account by organization and platform and account ID
     */
    Optional<SocialAccountEntity> findByOrganizationIdAndPlatformAndPlatformAccountId(
        UUID organizationId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Find social account by client and platform and account ID
     */
    Optional<SocialAccountEntity> findByClientIdAndPlatformAndPlatformAccountId(
        UUID clientId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Find all social accounts for an organization filtered by platform
     */
    List<SocialAccountEntity> findByOrganizationIdAndPlatform(UUID organizationId, SocialPlatform platform);

    /**
     * Find all social accounts for a client filtered by platform
     */
    List<SocialAccountEntity> findByClientIdAndPlatform(UUID clientId, SocialPlatform platform);

    /**
     * Find social account by ID and organization ID (for authorization checks)
     */
    Optional<SocialAccountEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Find social account by ID and client ID (for agency tenant isolation)
     */
    Optional<SocialAccountEntity> findByIdAndClientId(UUID id, UUID clientId);

    /**
     * Check if a social account exists for organization, platform, and platform account ID
     */
    boolean existsByOrganizationIdAndPlatformAndPlatformAccountId(
        UUID organizationId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Check if a social account exists for client, platform, and platform account ID
     */
    boolean existsByClientIdAndPlatformAndPlatformAccountId(
        UUID clientId,
        SocialPlatform platform,
        String platformAccountId
    );

    /**
     * Find all social accounts with tokens expiring before a certain date (for refresh scheduler)
     */
    @Query("SELECT sa FROM SocialAccountEntity sa WHERE sa.active = true AND sa.tokenExpiresAt < :expirationThreshold")
    List<SocialAccountEntity> findAccountsWithExpiringTokens(@Param("expirationThreshold") Instant expirationThreshold);

    /**
     * Find all social accounts with tokens that are already expired
     */
    @Query("SELECT sa FROM SocialAccountEntity sa WHERE sa.active = true AND sa.tokenExpiresAt < :now")
    List<SocialAccountEntity> findAccountsWithExpiredTokens(@Param("now") Instant now);

    /**
     * Find all active social accounts by platform (useful for batch operations)
     */
    List<SocialAccountEntity> findByPlatformAndActiveTrue(SocialPlatform platform);

    /**
     * Count social accounts for an organization
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Count social accounts for a client
     */
    long countByClientId(UUID clientId);

    /**
     * Count active social accounts for an organization by platform
     */
    long countByOrganizationIdAndPlatformAndActiveTrue(UUID organizationId, SocialPlatform platform);
}
