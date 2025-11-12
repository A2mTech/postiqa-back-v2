package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationInvitationEntity;
import fr.postiqa.gateway.auth.service.InvitationService;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.dto.auth.InviteMemberRequest;
import fr.postiqa.shared.dto.auth.InviteMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for inviting a member to an organization.
 */
@UseCase(
    value = "InviteMember",
    resourceType = "INVITATION",
    description = "Invites a new member to the organization"
)
@Service
@RequiredArgsConstructor
@Slf4j
public class InviteMemberUseCase implements fr.postiqa.shared.usecase.UseCase<InviteMemberRequest, InviteMemberResponse> {

    private final InvitationService invitationService;

    @Transactional
    public InviteMemberResponse execute(InviteMemberRequest request) {
        // Get invitedByUserId from tenant context (handled by UseCaseHandler)
        UUID invitedByUserId = fr.postiqa.gateway.auth.authorization.TenantContextHolder.getUserId();

        // Create invitation
        OrganizationInvitationEntity invitation = invitationService.createInvitation(
            request.getEmail(),
            request.getOrganizationId(),
            request.getRoleId(),
            request.getClientId(),
            invitedByUserId
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
