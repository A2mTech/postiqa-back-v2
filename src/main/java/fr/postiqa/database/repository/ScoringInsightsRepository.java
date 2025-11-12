package fr.postiqa.database.repository;

import fr.postiqa.database.entity.ScoringInsightsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ScoringInsightsEntity.
 * Provides data access methods for scoring and insights.
 */
@Repository
public interface ScoringInsightsRepository extends JpaRepository<ScoringInsightsEntity, UUID> {

    /**
     * Find scoring insights by user profile analysis ID
     */
    Optional<ScoringInsightsEntity> findByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Delete scoring insights by user profile analysis ID
     */
    void deleteByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Check if scoring insights exist for a user profile analysis
     */
    boolean existsByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Find all scoring insights with overall quality score above threshold
     */
    @Query("SELECT si FROM ScoringInsightsEntity si WHERE si.overallContentQualityScore >= :threshold")
    List<ScoringInsightsEntity> findByMinimumQualityScore(@Param("threshold") Integer threshold);

    /**
     * Find all scoring insights with thought leadership score above threshold
     */
    @Query("SELECT si FROM ScoringInsightsEntity si WHERE si.thoughtLeadershipScore >= :threshold")
    List<ScoringInsightsEntity> findByMinimumThoughtLeadershipScore(@Param("threshold") Integer threshold);
}
