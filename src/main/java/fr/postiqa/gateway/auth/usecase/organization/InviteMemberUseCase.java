package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationInvitationEntity;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.gateway.auth.service.InvitationService;
import fr.postiqa.shared.dto.auth.InviteMemberRequest;
import fr.postiqa.shared.dto.auth.InviteMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for inviting a member to an organization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InviteMemberUseCase {

    private final InvitationService invitationService;
    private final ActivityLogService activityLogService;

    @Transactional
    public InviteMemberResponse execute(InviteMemberRequest request, UUID invitedByUserId) {
        // Create invitation
        OrganizationInvitationEntity invitation = invitationService.createInvitation(
            request.getEmail(),
            request.getOrganizationId(),
            request.getRoleId(),
            request.getClientId(),
            invitedByUserId
        );

        // Log activity
        activityLogService.logActivity(
            invitedByUserId,
            request.getOrganizationId(),
            request.getClientId(),
            "MEMBER_INVITED",
            "INVITATION",
            invitation.getId(),
            null, // IP address should be passed from controller
            null, // User agent should be passed from controller
            Map.of(
                "email", request.getEmail(),
                "role", invitation.getRole().getName()
            )
        );

        log.info("Member invited: {} to organization {}", request.getEmail(), request.getOrganizationId());

        return InviteMemberResponse.builder()
            .invitationId(invitation.getId())
            .email(invitation.getEmail())
            .organizationName(invitation.getOrganization().getName())
            .roleName(invitation.getRole().getName())
            .expiresAt(invitation.getExpiresAt())
            .message("Invitation sent successfully")
            .build();
    }
}
