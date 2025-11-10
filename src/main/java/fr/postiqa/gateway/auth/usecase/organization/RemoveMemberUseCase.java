package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.repository.OrganizationMemberRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.gateway.auth.service.OrganizationMemberService;
import fr.postiqa.shared.exception.auth.CannotModifySelfException;
import fr.postiqa.shared.exception.auth.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for removing a member from an organization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemoveMemberUseCase {

    private final OrganizationMemberService memberService;
    private final OrganizationMemberRepository memberRepository;
    private final UserRoleRepository userRoleRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public void execute(UUID memberId, UUID removedByUserId, UUID organizationId) {
        OrganizationMemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        // Prevent self-removal
        if (member.getUser().getId().equals(removedByUserId)) {
            throw new CannotModifySelfException("Cannot remove yourself from the organization");
        }

        // Verify member belongs to the organization
        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new MemberNotFoundException("Member not found in this organization");
        }

        // Remove the member (sets status to REMOVED, unsets manager for direct reports)
        memberService.removeMember(memberId);

        // Remove user roles for this organization
        var userRoles = userRoleRepository.findByUserIdAndOrganizationId(
            member.getUser().getId(),
            organizationId
        );
        userRoleRepository.deleteAll(userRoles);

        // Log activity
        activityLogService.logActivity(
            removedByUserId,
            organizationId,
            null,
            "MEMBER_REMOVED",
            "MEMBER",
            memberId,
            null,
            null,
            Map.of(
                "removedUserEmail", member.getUser().getEmail(),
                "removedUserId", member.getUser().getId().toString()
            )
        );

        log.info("Member {} removed from organization {} by user {}",
            member.getUser().getEmail(), organizationId, removedByUserId);
    }
}
