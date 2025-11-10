package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.entity.RoleEntity;
import fr.postiqa.database.entity.UserRoleEntity;
import fr.postiqa.database.repository.OrganizationMemberRepository;
import fr.postiqa.database.repository.RoleRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.shared.dto.auth.UpdateMemberRoleRequest;
import fr.postiqa.shared.exception.auth.CannotModifySelfException;
import fr.postiqa.shared.exception.auth.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for updating a member's role.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateMemberRoleUseCase {

    private final OrganizationMemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public void execute(UUID memberId, UpdateMemberRoleRequest request, UUID updatedByUserId, UUID organizationId) {
        OrganizationMemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        // Prevent self-modification
        if (member.getUser().getId().equals(updatedByUserId)) {
            throw new CannotModifySelfException("Cannot modify your own role");
        }

        // Verify member belongs to the organization
        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new MemberNotFoundException("Member not found in this organization");
        }

        // Get new role
        RoleEntity newRole = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new RuntimeException("Role not found"));

        // Remove existing roles for this organization
        List<UserRoleEntity> existingRoles = userRoleRepository.findByUserIdAndOrganizationId(
            member.getUser().getId(),
            organizationId
        );

        String oldRoleName = existingRoles.isEmpty() ? "None" :
            existingRoles.get(0).getRole().getName();

        userRoleRepository.deleteAll(existingRoles);

        // Add new role
        UserRoleEntity newUserRole = UserRoleEntity.builder()
            .user(member.getUser())
            .role(newRole)
            .organization(member.getOrganization())
            .client(request.getClientId() != null ?
                existingRoles.stream()
                    .filter(ur -> ur.getClient() != null && ur.getClient().getId().equals(request.getClientId()))
                    .findFirst()
                    .map(UserRoleEntity::getClient)
                    .orElse(null) : null)
            .build();

        userRoleRepository.save(newUserRole);

        // Log activity
        activityLogService.logActivity(
            updatedByUserId,
            organizationId,
            request.getClientId(),
            "MEMBER_ROLE_UPDATED",
            "MEMBER",
            memberId,
            null,
            null,
            Map.of(
                "userEmail", member.getUser().getEmail(),
                "oldRole", oldRoleName,
                "newRole", newRole.getName()
            )
        );

        log.info("Updated role for member {} from {} to {}",
            member.getUser().getEmail(), oldRoleName, newRole.getName());
    }
}
