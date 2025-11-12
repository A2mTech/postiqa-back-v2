package fr.postiqa.database.repository;

import fr.postiqa.database.entity.SiteAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for SiteAnalysisEntity.
 * Provides data access methods for website analysis.
 */
@Repository
public interface SiteAnalysisRepository extends JpaRepository<SiteAnalysisEntity, UUID> {

    /**
     * Find site analysis by user profile analysis ID
     */
    Optional<SiteAnalysisEntity> findByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Delete site analysis by user profile analysis ID
     */
    void deleteByUserProfileAnalysisId(UUID userProfileAnalysisId);

    /**
     * Check if site analysis exists for a user profile analysis
     */
    boolean existsByUserProfileAnalysisId(UUID userProfileAnalysisId);
}
