package fr.postiqa.database.repository;

import fr.postiqa.database.entity.UserPermissionOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPermissionOverrideEntity.
 * Provides data access methods for custom permission grants/revocations.
 */
@Repository
public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverrideEntity, UUID> {

    /**
     * Find all permission overrides for a user in an organization
     */
    List<UserPermissionOverrideEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Find all permission overrides for a user across all organizations
     */
    List<UserPermissionOverrideEntity> findByUserId(UUID userId);

    /**
     * Find all permission overrides in an organization
     */
    List<UserPermissionOverrideEntity> findByOrganizationId(UUID organizationId);

    /**
     * Find specific permission override
     */
    Optional<UserPermissionOverrideEntity> findByUserIdAndOrganizationIdAndPermissionId(
        UUID userId,
        UUID organizationId,
        UUID permissionId
    );

    /**
     * Find all granted permissions for a user in an organization
     */
    List<UserPermissionOverrideEntity> findByUserIdAndOrganizationIdAndGranted(
        UUID userId,
        UUID organizationId,
        Boolean granted
    );

    /**
     * Find all permission overrides with permission details
     */
    @Query("SELECT po FROM UserPermissionOverrideEntity po " +
           "LEFT JOIN FETCH po.permission " +
           "WHERE po.userId = :userId AND po.organizationId = :organizationId")
    List<UserPermissionOverrideEntity> findByUserIdAndOrganizationIdWithPermission(
        @Param("userId") UUID userId,
        @Param("organizationId") UUID organizationId
    );

    /**
     * Check if a user has a permission override (granted or revoked)
     */
    boolean existsByUserIdAndOrganizationIdAndPermissionId(
        UUID userId,
        UUID organizationId,
        UUID permissionId
    );

    /**
     * Delete all permission overrides for a user in an organization
     */
    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Delete specific permission override
     */
    void deleteByUserIdAndOrganizationIdAndPermissionId(
        UUID userId,
        UUID organizationId,
        UUID permissionId
    );
}
