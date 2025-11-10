package fr.postiqa.database.repository;

import fr.postiqa.database.entity.OrganizationInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OrganizationInvitationEntity.
 * Provides data access methods for organization invitations.
 */
@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitationEntity, UUID> {

    /**
     * Find invitation by token
     */
    Optional<OrganizationInvitationEntity> findByToken(String token);

    /**
     * Find all invitations for an email
     */
    List<OrganizationInvitationEntity> findByEmail(String email);

    /**
     * Find all invitations for an email in a specific organization
     */
    List<OrganizationInvitationEntity> findByEmailAndOrganizationId(String email, UUID organizationId);

    /**
     * Find all invitations for an organization
     */
    List<OrganizationInvitationEntity> findByOrganizationId(UUID organizationId);

    /**
     * Find all invitations with a specific status
     */
    List<OrganizationInvitationEntity> findByStatus(OrganizationInvitationEntity.InvitationStatus status);

    /**
     * Find all pending invitations for an organization
     */
    List<OrganizationInvitationEntity> findByOrganizationIdAndStatus(
        UUID organizationId,
        OrganizationInvitationEntity.InvitationStatus status
    );

    /**
     * Find pending invitation by email and organization
     */
    Optional<OrganizationInvitationEntity> findByEmailAndOrganizationIdAndStatus(
        String email,
        UUID organizationId,
        OrganizationInvitationEntity.InvitationStatus status
    );

    /**
     * Find all expired invitations that are still pending
     */
    @Query("SELECT i FROM OrganizationInvitationEntity i " +
           "WHERE i.status = :status AND i.expiresAt < :now")
    List<OrganizationInvitationEntity> findExpiredInvitations(
        @Param("status") OrganizationInvitationEntity.InvitationStatus status,
        @Param("now") Instant now
    );

    /**
     * Check if an active invitation exists for an email in an organization
     */
    boolean existsByEmailAndOrganizationIdAndStatus(
        String email,
        UUID organizationId,
        OrganizationInvitationEntity.InvitationStatus status
    );

    /**
     * Delete all expired invitations
     */
    void deleteByStatusAndExpiresAtBefore(
        OrganizationInvitationEntity.InvitationStatus status,
        Instant expiresAt
    );
}
