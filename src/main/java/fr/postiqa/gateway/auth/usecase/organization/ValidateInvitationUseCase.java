package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationInvitationEntity;
import fr.postiqa.gateway.auth.service.InvitationService;
import fr.postiqa.shared.dto.auth.InvitationDto;
import fr.postiqa.shared.dto.auth.OrganizationDto;
import fr.postiqa.shared.dto.auth.RoleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for validating an invitation token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateInvitationUseCase {

    private final InvitationService invitationService;

    @Transactional(readOnly = true)
    public InvitationDto execute(String token) {
        OrganizationInvitationEntity invitation = invitationService.validateInvitation(token);

        return InvitationDto.builder()
            .id(invitation.getId())
            .email(invitation.getEmail())
            .organization(OrganizationDto.builder()
                .id(invitation.getOrganization().getId())
                .name(invitation.getOrganization().getName())
                .type(invitation.getOrganization().getType().name())
                .build())
            .role(RoleDto.builder()
                .id(invitation.getRole().getId())
                .name(invitation.getRole().getName())
                .description(invitation.getRole().getDescription())
                .build())
            .status(invitation.getStatus().name())
            .expiresAt(invitation.getExpiresAt())
            .createdAt(invitation.getCreatedAt())
            .build();
    }
}
