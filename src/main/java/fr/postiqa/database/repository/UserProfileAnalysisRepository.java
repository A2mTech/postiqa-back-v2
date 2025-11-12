package fr.postiqa.database.repository;

import fr.postiqa.database.entity.UserProfileAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserProfileAnalysisEntity.
 * Provides data access methods for ultra-deep profile analysis management.
 */
@Repository
public interface UserProfileAnalysisRepository extends JpaRepository<UserProfileAnalysisEntity, UUID> {

    /**
     * Find all analyses for a specific user ID
     */
    List<UserProfileAnalysisEntity> findByUserId(String userId);

    /**
     * Find all analyses for a specific organization
     */
    List<UserProfileAnalysisEntity> findByOrganizationId(UUID organizationId);

    /**
     * Find all analyses for a specific client
     */
    List<UserProfileAnalysisEntity> findByClientId(UUID clientId);

    /**
     * Find analysis by workflow instance ID
     */
    Optional<UserProfileAnalysisEntity> findByWorkflowInstanceId(String workflowInstanceId);

    /**
     * Find all analyses with a specific status
     */
    List<UserProfileAnalysisEntity> findByStatus(String status);

    /**
     * Find all analyses for an organization with a specific status
     */
    List<UserProfileAnalysisEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    /**
     * Find all analyses for a client with a specific status
     */
    List<UserProfileAnalysisEntity> findByClientIdAndStatus(UUID clientId, String status);

    /**
     * Find analysis by ID and organization ID (for authorization checks)
     */
    Optional<UserProfileAnalysisEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Find analysis by ID and client ID (for agency tenant isolation)
     */
    Optional<UserProfileAnalysisEntity> findByIdAndClientId(UUID id, UUID clientId);

    /**
     * Find all analyses created after a specific timestamp
     */
    List<UserProfileAnalysisEntity> findByCreatedAtAfter(Instant timestamp);

    /**
     * Find all completed analyses for an organization
     */
    @Query("SELECT upa FROM UserProfileAnalysisEntity upa WHERE upa.organization.id = :organizationId AND upa.status = 'COMPLETED' ORDER BY upa.completedAt DESC")
    List<UserProfileAnalysisEntity> findCompletedAnalysesByOrganization(@Param("organizationId") UUID organizationId);

    /**
     * Find all completed analyses for a client
     */
    @Query("SELECT upa FROM UserProfileAnalysisEntity upa WHERE upa.client.id = :clientId AND upa.status = 'COMPLETED' ORDER BY upa.completedAt DESC")
    List<UserProfileAnalysisEntity> findCompletedAnalysesByClient(@Param("clientId") UUID clientId);

    /**
     * Find all running analyses (not terminal states)
     */
    @Query("SELECT upa FROM UserProfileAnalysisEntity upa WHERE upa.status IN ('PENDING', 'RUNNING')")
    List<UserProfileAnalysisEntity> findAllActive();

    /**
     * Count analyses by organization
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Count analyses by client
     */
    long countByClientId(UUID clientId);

    /**
     * Count analyses by organization and status
     */
    long countByOrganizationIdAndStatus(UUID organizationId, String status);

    /**
     * Count analyses by client and status
     */
    long countByClientIdAndStatus(UUID clientId, String status);

    /**
     * Check if an analysis exists for a user and organization
     */
    boolean existsByUserIdAndOrganizationId(String userId, UUID organizationId);

    /**
     * Find the most recent analysis for a user in an organization
     */
    @Query("SELECT upa FROM UserProfileAnalysisEntity upa WHERE upa.userId = :userId AND upa.organization.id = :organizationId ORDER BY upa.createdAt DESC LIMIT 1")
    Optional<UserProfileAnalysisEntity> findMostRecentAnalysisForUser(@Param("userId") String userId, @Param("organizationId") UUID organizationId);
}
