package fr.postiqa.gateway.auth.authorization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.OrganizationMemberRepository;
import fr.postiqa.shared.exception.auth.HierarchyCycleException;
import fr.postiqa.shared.exception.auth.InvalidManagerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Validator for organizational hierarchy relationships.
 * Ensures hierarchy integrity and prevents cycles.
 */
@Component
@RequiredArgsConstructor
public class HierarchyValidator {

    private final OrganizationMemberRepository memberRepository;

    /**
     * Validate that setting a manager doesn't create a cycle.
     * A cycle would occur if the manager is a subordinate (direct or indirect) of the member.
     *
     * @param memberId The member whose manager is being set
     * @param managerId The proposed manager
     * @param organizationId The organization context
     * @throws HierarchyCycleException if a cycle would be created
     * @throws InvalidManagerException if manager is not in same organization
     */
    public void validateNoCycle(UUID memberId, UUID managerId, UUID organizationId) {
        if (memberId.equals(managerId)) {
            throw new HierarchyCycleException("A member cannot be their own manager");
        }

        // Verify manager is in the same organization
        Optional<OrganizationMemberEntity> managerMember =
            memberRepository.findByUserIdAndOrganizationId(managerId, organizationId);

        if (managerMember.isEmpty()) {
            throw new InvalidManagerException("Manager must be a member of the same organization");
        }

        // Check if setting this manager would create a cycle
        // by walking up the manager chain from the proposed manager
        Set<UUID> visited = new HashSet<>();
        UUID currentUserId = managerId;

        while (currentUserId != null) {
            if (currentUserId.equals(memberId)) {
                throw new HierarchyCycleException(
                    "Cannot set this manager as it would create a circular hierarchy"
                );
            }

            if (visited.contains(currentUserId)) {
                // Already visited this node, stop to prevent infinite loop
                break;
            }

            visited.add(currentUserId);

            // Get the manager of the current user
            Optional<OrganizationMemberEntity> currentMember =
                memberRepository.findByUserIdAndOrganizationId(currentUserId, organizationId);

            currentUserId = currentMember
                .flatMap(m -> Optional.ofNullable(m.getManager()))
                .map(UserEntity::getId)
                .orElse(null);
        }
    }

    /**
     * Check if user A is a manager (direct or indirect) of user B.
     *
     * @param potentialManagerId The potential manager
     * @param subordinateId The potential subordinate
     * @param organizationId The organization context
     * @return true if potentialManager is above subordinate in hierarchy
     */
    public boolean isManagerOf(UUID potentialManagerId, UUID subordinateId, UUID organizationId) {
        if (potentialManagerId.equals(subordinateId)) {
            return false;
        }

        Optional<OrganizationMemberEntity> subordinateMember =
            memberRepository.findByUserIdAndOrganizationId(subordinateId, organizationId);

        if (subordinateMember.isEmpty()) {
            return false;
        }

        Set<UUID> visited = new HashSet<>();
        UUID currentManagerId = subordinateMember.get().getManager() != null
            ? subordinateMember.get().getManager().getId()
            : null;

        while (currentManagerId != null) {
            if (currentManagerId.equals(potentialManagerId)) {
                return true;
            }

            if (visited.contains(currentManagerId)) {
                break;
            }

            visited.add(currentManagerId);

            Optional<OrganizationMemberEntity> currentManager =
                memberRepository.findByUserIdAndOrganizationId(currentManagerId, organizationId);

            currentManagerId = currentManager
                .flatMap(m -> Optional.ofNullable(m.getManager()))
                .map(UserEntity::getId)
                .orElse(null);
        }

        return false;
    }

    /**
     * Get all subordinates (direct and indirect) of a manager.
     *
     * @param managerId The manager
     * @param organizationId The organization context
     * @return Set of all subordinate user IDs
     */
    public Set<UUID> getAllSubordinates(UUID managerId, UUID organizationId) {
        Set<UUID> allSubordinates = new HashSet<>();
        collectSubordinates(managerId, organizationId, allSubordinates);
        return allSubordinates;
    }

    private void collectSubordinates(UUID managerId, UUID organizationId, Set<UUID> collected) {
        // Get direct reports
        var directReports = memberRepository.findByManagerIdAndOrganizationId(managerId, organizationId);

        for (OrganizationMemberEntity report : directReports) {
            UUID reportId = report.getUser().getId();
            if (!collected.contains(reportId)) {
                collected.add(reportId);
                // Recursively collect their subordinates
                collectSubordinates(reportId, organizationId, collected);
            }
        }
    }

    /**
     * Validate that a user can manage another member.
     * A user can manage a member if:
     * - They are the direct manager
     * - They are an indirect manager (higher in hierarchy)
     *
     * @param userId The user performing the action
     * @param targetMemberId The member being managed
     * @param organizationId The organization context
     * @return true if user can manage the member
     */
    public boolean canManageMember(UUID userId, UUID targetMemberId, UUID organizationId) {
        if (userId.equals(targetMemberId)) {
            return false; // Cannot manage self
        }

        return isManagerOf(userId, targetMemberId, organizationId);
    }
}
