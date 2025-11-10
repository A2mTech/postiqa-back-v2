package fr.postiqa.gateway.auth.service;

import fr.postiqa.database.entity.*;
import fr.postiqa.database.repository.*;
import fr.postiqa.shared.exception.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing organization invitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final OrganizationInvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    private static final int INVITATION_EXPIRY_DAYS = 7;

    /**
     * Create a new invitation.
     */
    @Transactional
    public OrganizationInvitationEntity createInvitation(
        String email,
        UUID organizationId,
        UUID roleId,
        UUID clientId,
        UUID invitedByUserId
    ) {
        // Validate organization exists
        OrganizationEntity organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        // Validate role exists
        RoleEntity role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found"));

        // Validate client if specified
        ClientEntity client = null;
        if (clientId != null) {
            client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

            // Ensure client belongs to organization (for agencies)
            if (!client.getAgency().getId().equals(organizationId)) {
                throw new RuntimeException("Client does not belong to this organization");
            }
        }

        // Validate inviter exists
        UserEntity invitedBy = userRepository.findById(invitedByUserId)
            .orElseThrow(() -> new UserNotFoundException("Inviting user not found"));

        // Check if pending invitation already exists
        boolean existingInvitation = invitationRepository.existsByEmailAndOrganizationIdAndStatus(
            email,
            organizationId,
            OrganizationInvitationEntity.InvitationStatus.PENDING
        );

        if (existingInvitation) {
            throw new InvitationAlreadyExistsException(
                "A pending invitation already exists for this email in this organization"
            );
        }

        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Calculate expiration
        Instant expiresAt = Instant.now().plus(INVITATION_EXPIRY_DAYS, ChronoUnit.DAYS);

        OrganizationInvitationEntity invitation = OrganizationInvitationEntity.builder()
            .email(email)
            .organization(organization)
            .role(role)
            .client(client)
            .invitedBy(invitedBy)
            .token(token)
            .expiresAt(expiresAt)
            .status(OrganizationInvitationEntity.InvitationStatus.PENDING)
            .build();

        log.info("Creating invitation for {} to organization {} with role {}",
            email, organization.getName(), role.getName());

        return invitationRepository.save(invitation);
    }

    /**
     * Find invitation by token.
     */
    @Transactional(readOnly = true)
    public OrganizationInvitationEntity findByToken(String token) {
        return invitationRepository.findByToken(token)
            .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));
    }

    /**
     * Validate and get invitation for acceptance.
     */
    @Transactional(readOnly = true)
    public OrganizationInvitationEntity validateInvitation(String token) {
        OrganizationInvitationEntity invitation = findByToken(token);

        // Check if already accepted
        if (invitation.getStatus() == OrganizationInvitationEntity.InvitationStatus.ACCEPTED) {
            throw new InvitationExpiredException("Invitation has already been accepted");
        }

        // Check if revoked
        if (invitation.getStatus() == OrganizationInvitationEntity.InvitationStatus.REVOKED) {
            throw new InvitationExpiredException("Invitation has been revoked");
        }

        // Check if expired
        if (Instant.now().isAfter(invitation.getExpiresAt())) {
            throw new InvitationExpiredException("Invitation has expired");
        }

        return invitation;
    }

    /**
     * Mark invitation as accepted.
     */
    @Transactional
    public void markAsAccepted(UUID invitationId, UUID acceptedByUserId) {
        OrganizationInvitationEntity invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        UserEntity acceptedBy = userRepository.findById(acceptedByUserId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        invitation.setStatus(OrganizationInvitationEntity.InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setAcceptedBy(acceptedBy);

        invitationRepository.save(invitation);
        log.info("Invitation {} accepted by user {}", invitationId, acceptedByUserId);
    }

    /**
     * Revoke a pending invitation.
     */
    @Transactional
    public void revokeInvitation(UUID invitationId) {
        OrganizationInvitationEntity invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        if (invitation.getStatus() != OrganizationInvitationEntity.InvitationStatus.PENDING) {
            throw new RuntimeException("Only pending invitations can be revoked");
        }

        invitation.setStatus(OrganizationInvitationEntity.InvitationStatus.REVOKED);
        invitationRepository.save(invitation);

        log.info("Invitation {} revoked", invitationId);
    }

    /**
     * Get all pending invitations for an organization.
     */
    @Transactional(readOnly = true)
    public List<OrganizationInvitationEntity> getPendingInvitations(UUID organizationId) {
        return invitationRepository.findByOrganizationIdAndStatus(
            organizationId,
            OrganizationInvitationEntity.InvitationStatus.PENDING
        );
    }

    /**
     * Cleanup expired invitations (scheduled task).
     */
    @Transactional
    public void cleanupExpiredInvitations() {
        List<OrganizationInvitationEntity> expired = invitationRepository.findExpiredInvitations(
            OrganizationInvitationEntity.InvitationStatus.PENDING,
            Instant.now()
        );

        expired.forEach(invitation -> {
            invitation.setStatus(OrganizationInvitationEntity.InvitationStatus.EXPIRED);
        });

        if (!expired.isEmpty()) {
            invitationRepository.saveAll(expired);
            log.info("Marked {} expired invitations", expired.size());
        }
    }
}
