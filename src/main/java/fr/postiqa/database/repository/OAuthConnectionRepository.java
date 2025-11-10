package fr.postiqa.database.repository;

import fr.postiqa.database.entity.OAuthConnectionEntity;
import fr.postiqa.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuthConnectionEntity.
 * Provides data access methods for OAuth2 connection management.
 */
@Repository
public interface OAuthConnectionRepository extends JpaRepository<OAuthConnectionEntity, UUID> {

    /**
     * Find OAuth connection by provider and provider user ID
     */
    Optional<OAuthConnectionEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Find all OAuth connections for a user
     */
    List<OAuthConnectionEntity> findByUser(UserEntity user);

    /**
     * Find all OAuth connections for a user by user ID
     */
    List<OAuthConnectionEntity> findByUserId(UUID userId);

    /**
     * Find OAuth connection by user and provider
     */
    Optional<OAuthConnectionEntity> findByUserAndProvider(UserEntity user, String provider);

    /**
     * Find OAuth connection by user ID and provider
     */
    Optional<OAuthConnectionEntity> findByUserIdAndProvider(UUID userId, String provider);

    /**
     * Check if OAuth connection exists for provider and provider user ID
     */
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
