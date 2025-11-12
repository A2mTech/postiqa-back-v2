package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PostAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PostAnalysisEntity.
 * Provides data access methods for post analysis.
 */
@Repository
public interface PostAnalysisRepository extends JpaRepository<PostAnalysisEntity, UUID> {

    /**
     * Find all post analyses for a platform profile
     */
    List<PostAnalysisEntity> findByPlatformProfileId(UUID platformProfileId);

    /**
     * Find post analysis by post ID
     */
    Optional<PostAnalysisEntity> findByPostId(String postId);

    /**
     * Find all post analyses for a platform profile by post type
     */
    List<PostAnalysisEntity> findByPlatformProfileIdAndPostType(UUID platformProfileId, String postType);

    /**
     * Delete all post analyses for a platform profile
     */
    void deleteByPlatformProfileId(UUID platformProfileId);

    /**
     * Count post analyses for a platform profile
     */
    long countByPlatformProfileId(UUID platformProfileId);

    /**
     * Count post analyses by platform profile and post type
     */
    long countByPlatformProfileIdAndPostType(UUID platformProfileId, String postType);

    /**
     * Find all post analyses for a user profile analysis (via platform profile)
     */
    @Query("SELECT pa FROM PostAnalysisEntity pa WHERE pa.platformProfile.userProfileAnalysis.id = :userProfileAnalysisId")
    List<PostAnalysisEntity> findByUserProfileAnalysisId(@Param("userProfileAnalysisId") UUID userProfileAnalysisId);

    /**
     * Count all post analyses for a user profile analysis
     */
    @Query("SELECT COUNT(pa) FROM PostAnalysisEntity pa WHERE pa.platformProfile.userProfileAnalysis.id = :userProfileAnalysisId")
    long countByUserProfileAnalysisId(@Param("userProfileAnalysisId") UUID userProfileAnalysisId);

    /**
     * Find top performing posts for a platform profile (by engagement)
     */
    @Query(value = "SELECT * FROM post_analyses WHERE platform_profile_id = :platformProfileId ORDER BY (engagement->>'likes')::int DESC LIMIT :limit", nativeQuery = true)
    List<PostAnalysisEntity> findTopPerformingPosts(@Param("platformProfileId") UUID platformProfileId, @Param("limit") int limit);
}
