package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PlatformProfileAnalysisEntity;
import fr.postiqa.shared.enums.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PlatformProfileAnalysisEntity.
 * Provides data access methods for platform profile analysis.
 */
@Repository
public interface PlatformProfileAnalysisRepository extends JpaRepository<PlatformProfileAnalysisEntity, UUID> {

    /**
     * Find all platform profiles for a user profile analysis
     */
    List<PlatformProfileAnalysisEntity> findByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Find platform profile by user profile analysis ID and platform
     */
    Optional<PlatformProfileAnalysisEntity> findByUserProfileAnalysisIdAndPlatform(UUID userProfileAnalysisId, SocialPlatform platform);

    /**
     * Delete all platform profiles for a user profile analysis
     */
    void deleteByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Count platform profiles for a user profile analysis
     */
    long countByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Check if platform profile exists for a user profile analysis and platform
     */
    boolean existsByUserProfileAnalysisIdAndPlatform(UUID userProfileAnalysisId, SocialPlatform platform);

    /**
     * Find all platform profiles for a specific platform
     */
    List<PlatformProfileAnalysisEntity> findByPlatform(SocialPlatform platform);
}
