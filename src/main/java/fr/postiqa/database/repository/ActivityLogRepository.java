package fr.postiqa.database.repository;

import fr.postiqa.database.entity.ActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ActivityLogEntity.
 * Provides data access methods for activity logs and audit trail.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, UUID> {

    /**
     * Find all activity logs for a user
     */
    Page<ActivityLogEntity> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all activity logs in an organization
     */
    Page<ActivityLogEntity> findByOrganizationId(UUID organizationId, Pageable pageable);

    /**
     * Find all activity logs for a user in an organization
     */
    Page<ActivityLogEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId, Pageable pageable);

    /**
     * Find all activity logs for a client
     */
    Page<ActivityLogEntity> findByClientId(UUID clientId, Pageable pageable);

    /**
     * Find activity logs by action
     */
    Page<ActivityLogEntity> findByOrganizationIdAndAction(
        UUID organizationId,
        String action,
        Pageable pageable
    );

    /**
     * Find activity logs by resource type
     */
    Page<ActivityLogEntity> findByOrganizationIdAndResourceType(
        UUID organizationId,
        String resourceType,
        Pageable pageable
    );

    /**
     * Find activity logs for a specific resource
     */
    List<ActivityLogEntity> findByResourceTypeAndResourceId(String resourceType, UUID resourceId);

    /**
     * Find activity logs in a time range
     */
    @Query("SELECT a FROM ActivityLogEntity a " +
           "WHERE a.organizationId = :organizationId " +
           "AND a.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.timestamp DESC")
    Page<ActivityLogEntity> findByOrganizationIdAndTimestampBetween(
        @Param("organizationId") UUID organizationId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );

    /**
     * Find recent activity logs for a user's direct reports
     */
    @Query("SELECT a FROM ActivityLogEntity a " +
           "WHERE a.userId IN :userIds " +
           "AND a.organizationId = :organizationId " +
           "ORDER BY a.timestamp DESC")
    Page<ActivityLogEntity> findRecentActivitiesForUsers(
        @Param("userIds") List<UUID> userIds,
        @Param("organizationId") UUID organizationId,
        Pageable pageable
    );

    /**
     * Count activities by user in organization
     */
    long countByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Count activities by action in organization
     */
    long countByOrganizationIdAndAction(UUID organizationId, String action);

    /**
     * Delete old activity logs (for cleanup)
     */
    void deleteByTimestampBefore(Instant timestamp);
}
