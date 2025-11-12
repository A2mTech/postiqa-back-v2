package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.entity.UserRoleEntity;
import fr.postiqa.database.repository.UserPermissionOverrideRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.service.OrganizationMemberService;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.dto.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for getting all members of an organization.
 */
@UseCase(
    value = "GetOrganizationMembers",
    resourceType = "MEMBER",
    description = "Retrieves all members of an organization",
    logActivity = false
)
@Service
@RequiredArgsConstructor
@Slf4j
public class GetOrganizationMembersUseCase implements fr.postiqa.shared.usecase.UseCase<UUID, List<OrganizationMemberDto>> {

    private final OrganizationMemberService memberService;
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionOverrideRepository permissionOverrideRepository;

    @Transactional(readOnly = true)
    public List<OrganizationMemberDto> execute(UUID organizationId) {
        List<OrganizationMemberEntity> members = memberService.getActiveMembers(organizationId);

        return members.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private OrganizationMemberDto toDto(OrganizationMemberEntity member) {
        // Get user roles for this organization
        List<UserRoleEntity> roles = userRoleRepository.findByUserIdAndOrganizationId(
            member.getUser().getId(),
            member.getOrganization().getId()
        );

        List<UserRoleDto> roleDtos = roles.stream()
            .map(ur -> UserRoleDto.builder()
                .role(RoleDto.builder()
                    .id(ur.getRole().getId())
                    .name(ur.getRole().getName())
                    .description(ur.getRole().getDescription())
                    .build())
                .organization(OrganizationDto.builder()
                    .id(ur.getOrganization().getId())
                    .name(ur.getOrganization().getName())
                    .type(ur.getOrganization().getType().name())
                    .build())
                .client(ur.getClient() != null ? ClientDto.builder()
                    .id(ur.getClient().getId())
                    .clientName(ur.getClient().getClientName())
                    .build() : null)
                .build())
            .collect(Collectors.toList());

        // Get permission overrides
        var overrides = permissionOverrideRepository.findByUserIdAndOrganizationIdWithPermission(
            member.getUser().getId(),
            member.getOrganization().getId()
        );

        List<PermissionOverrideDto> overrideDtos = overrides.stream()
            .map(po -> PermissionOverrideDto.builder()
                .id(po.getId())
                .permission(PermissionDto.builder()
                    .id(po.getPermission().getId())
                    .resource(po.getPermission().getResource())
                    .action(po.getPermission().getAction())
                    .build())
                .granted(po.getGranted())
                .reason(po.getReason())
                .createdAt(po.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return OrganizationMemberDto.builder()
            .id(member.getId())
            .user(UserDto.builder()
                .id(member.getUser().getId())
                .email(member.getUser().getEmail())
                .firstName(member.getUser().getFirstName())
                .lastName(member.getUser().getLastName())
                .emailVerified(member.getUser().getEmailVerified())
                .enabled(member.getUser().getEnabled())
                .build())
            .organization(OrganizationDto.builder()
                .id(member.getOrganization().getId())
                .name(member.getOrganization().getName())
                .type(member.getOrganization().getType().name())
                .build())
            .manager(member.getManager() != null ? UserDto.builder()
                .id(member.getManager().getId())
                .email(member.getManager().getEmail())
                .firstName(member.getManager().getFirstName())
                .lastName(member.getManager().getLastName())
                .build() : null)
            .position(member.getPosition())
            .title(member.getTitle())
            .status(member.getStatus().name())
            .joinedAt(member.getJoinedAt())
            .createdAt(member.getCreatedAt())
            .roles(roleDtos)
            .permissionOverrides(overrideDtos)
            .build();
    }
}
