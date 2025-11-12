package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PlatformSummaryEntity;
import fr.postiqa.shared.enums.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PlatformSummaryEntity.
 * Provides data access methods for platform summary.
 */
@Repository
public interface PlatformSummaryRepository extends JpaRepository<PlatformSummaryEntity, UUID> {

    /**
     * Find all platform summaries for a user profile analysis
     */
    List<PlatformSummaryEntity> findByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Find platform summary by user profile analysis ID and platform
     */
    Optional<PlatformSummaryEntity> findByUserProfileAnalysisIdAndPlatform(UUID userProfileAnalysisId, SocialPlatform platform);

    /**
     * Delete all platform summaries for a user profile analysis
     */
    void deleteByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Count platform summaries for a user profile analysis
     */
    long countByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Check if platform summary exists for a user profile analysis and platform
     */
    boolean existsByUserProfileAnalysisIdAndPlatform(UUID userProfileAnalysisId, SocialPlatform platform);

    /**
     * Find all platform summaries for a specific platform
     */
    List<PlatformSummaryEntity> findByPlatform(SocialPlatform platform);
}
