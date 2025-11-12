package fr.postiqa.database.repository;

import fr.postiqa.database.entity.CrossReferenceAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CrossReferenceAnalysisEntity.
 * Provides data access methods for cross-reference analysis.
 */
@Repository
public interface CrossReferenceAnalysisRepository extends JpaRepository<CrossReferenceAnalysisEntity, UUID> {

    /**
     * Find cross-reference analysis by user profile analysis ID
     */
    Optional<CrossReferenceAnalysisEntity> findByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Delete cross-reference analysis by user profile analysis ID
     */
    void deleteByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Check if cross-reference analysis exists for a user profile analysis
     */
    boolean existsByUserProfileAnalysisId(UUID userProfileAnalysisId);
}
