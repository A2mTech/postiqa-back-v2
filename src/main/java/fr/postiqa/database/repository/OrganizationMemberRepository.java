package fr.postiqa.database.repository;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OrganizationMemberEntity.
 * Provides data access methods for organization members and hierarchy.
 */
@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, UUID> {

    /**
     * Find member by user and organization
     */
    Optional<OrganizationMemberEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Find all members in an organization
     */
    List<OrganizationMemberEntity> findByOrganizationId(UUID organizationId);

    /**
     * Find all members with a specific status in an organization
     */
    List<OrganizationMemberEntity> findByOrganizationIdAndStatus(UUID organizationId, OrganizationMemberEntity.MemberStatus status);

    /**
     * Find all direct reports of a manager
     */
    List<OrganizationMemberEntity> findByManagerId(UUID managerId);

    /**
     * Find all direct reports of a manager in a specific organization
     */
    List<OrganizationMemberEntity> findByManagerIdAndOrganizationId(UUID managerId, UUID organizationId);

    /**
     * Find member with full hierarchy (manager, organization, user)
     */
    @Query("SELECT m FROM OrganizationMemberEntity m " +
           "LEFT JOIN FETCH m.manager " +
           "LEFT JOIN FETCH m.organization " +
           "LEFT JOIN FETCH m.user " +
           "WHERE m.userId = :userId AND m.organizationId = :organizationId")
    Optional<OrganizationMemberEntity> findByUserIdAndOrganizationIdWithHierarchy(
        @Param("userId") UUID userId,
        @Param("organizationId") UUID organizationId
    );

    /**
     * Check if a user is a member of an organization
     */
    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Check if a user is a member of an organization with specific status
     */
    boolean existsByUserIdAndOrganizationIdAndStatus(
        UUID userId,
        UUID organizationId,
        OrganizationMemberEntity.MemberStatus status
    );

    /**
     * Count active members in an organization
     */
    long countByOrganizationIdAndStatus(UUID organizationId, OrganizationMemberEntity.MemberStatus status);
}
