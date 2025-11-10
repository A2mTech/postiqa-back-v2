package fr.postiqa.database.repository;

import fr.postiqa.database.entity.ApiKeyEntity;
import fr.postiqa.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApiKeyEntity.
 * Provides data access methods for API key management.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    /**
     * Find API key by hash
     */
    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    /**
     * Find all API keys for a user
     */
    List<ApiKeyEntity> findByUser(UserEntity user);

    /**
     * Find all API keys for a user by user ID
     */
    List<ApiKeyEntity> findByUserId(UUID userId);

    /**
     * Find all active API keys for a user
     */
    List<ApiKeyEntity> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find active API key by hash
     */
    Optional<ApiKeyEntity> findByKeyHashAndActiveTrue(String keyHash);
}
