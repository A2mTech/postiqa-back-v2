package fr.postiqa.gateway.auth.service;

import fr.postiqa.database.entity.*;
import fr.postiqa.database.repository.*;
import fr.postiqa.gateway.auth.authorization.HierarchyValidator;
import fr.postiqa.shared.exception.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing organization members.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationMemberService {

    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final HierarchyValidator hierarchyValidator;

    /**
     * Create a new organization member.
     */
    @Transactional
    public OrganizationMemberEntity createMember(
        UUID userId,
        UUID organizationId,
        UUID managerId,
        UUID invitedByUserId,
        String position,
        String title
    ) {
        // Validate organization exists
        OrganizationEntity organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        // Validate user exists
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if already a member
        if (memberRepository.existsByUserIdAndOrganizationId(userId, organizationId)) {
            throw new MemberAlreadyExistsException("User is already a member of this organization");
        }

        // Validate manager if specified
        UserEntity manager = null;
        if (managerId != null) {
            manager = userRepository.findById(managerId)
                .orElseThrow(() -> new UserNotFoundException("Manager not found"));

            // Validate no cycle
            hierarchyValidator.validateNoCycle(userId, managerId, organizationId);
        }

        // Validate inviter exists
        UserEntity invitedBy = invitedByUserId != null
            ? userRepository.findById(invitedByUserId)
                .orElseThrow(() -> new UserNotFoundException("Inviting user not found"))
            : null;

        OrganizationMemberEntity member = OrganizationMemberEntity.builder()
            .user(user)
            .organization(organization)
            .manager(manager)
            .position(position)
            .title(title)
            .status(OrganizationMemberEntity.MemberStatus.ACTIVE)
            .invitedBy(invitedBy)
            .joinedAt(Instant.now())
            .build();

        log.info("Creating member {} in organization {}", user.getEmail(), organization.getName());

        return memberRepository.save(member);
    }

    /**
     * Update member's manager.
     */
    @Transactional
    public void setManager(UUID memberId, UUID managerId, UUID organizationId) {
        OrganizationMemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new MemberNotFoundException("Member not found in this organization");
        }

        if (managerId != null) {
            UserEntity manager = userRepository.findById(managerId)
                .orElseThrow(() -> new UserNotFoundException("Manager not found"));

            // Validate no cycle
            hierarchyValidator.validateNoCycle(member.getUser().getId(), managerId, organizationId);

            member.setManager(manager);
        } else {
            member.setManager(null);
        }

        memberRepository.save(member);
        log.info("Updated manager for member {} to {}", memberId, managerId);
    }

    /**
     * Update member information.
     */
    @Transactional
    public void updateMember(UUID memberId, String position, String title, OrganizationMemberEntity.MemberStatus status) {
        OrganizationMemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        if (position != null) {
            member.setPosition(position);
        }
        if (title != null) {
            member.setTitle(title);
        }
        if (status != null) {
            member.setStatus(status);
        }

        memberRepository.save(member);
        log.info("Updated member {}", memberId);
    }

    /**
     * Remove a member from organization.
     */
    @Transactional
    public void removeMember(UUID memberId) {
        OrganizationMemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        // Find all direct reports and unset their manager
        List<OrganizationMemberEntity> directReports = memberRepository.findByManagerId(member.getUser().getId());
        directReports.forEach(report -> {
            report.setManager(null);
            memberRepository.save(report);
        });

        member.setStatus(OrganizationMemberEntity.MemberStatus.REMOVED);
        memberRepository.save(member);

        log.info("Removed member {}", memberId);
    }

    /**
     * Get all members of an organization.
     */
    @Transactional(readOnly = true)
    public List<OrganizationMemberEntity> getOrganizationMembers(UUID organizationId) {
        return memberRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get active members of an organization.
     */
    @Transactional(readOnly = true)
    public List<OrganizationMemberEntity> getActiveMembers(UUID organizationId) {
        return memberRepository.findByOrganizationIdAndStatus(
            organizationId,
            OrganizationMemberEntity.MemberStatus.ACTIVE
        );
    }

    /**
     * Get member by user and organization.
     */
    @Transactional(readOnly = true)
    public OrganizationMemberEntity getMember(UUID userId, UUID organizationId) {
        return memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));
    }

    /**
     * Get direct reports of a manager.
     */
    @Transactional(readOnly = true)
    public List<OrganizationMemberEntity> getDirectReports(UUID managerId, UUID organizationId) {
        return memberRepository.findByManagerIdAndOrganizationId(managerId, organizationId);
    }
}
