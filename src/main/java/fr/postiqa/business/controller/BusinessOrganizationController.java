package fr.postiqa.business.controller;

import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.gateway.auth.usecase.organization.*;
import fr.postiqa.shared.dto.auth.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for business organization management.
 * Handles organization member management for business accounts.
 */
@RestController
@RequestMapping("/api/business/organization")
@RequiredArgsConstructor
@Slf4j
public class BusinessOrganizationController {

    private final InviteMemberUseCase inviteMemberUseCase;
    private final CreateMemberDirectlyUseCase createMemberDirectlyUseCase;
    private final GetOrganizationMembersUseCase getOrganizationMembersUseCase;
    private final GetOrganizationHierarchyUseCase getOrganizationHierarchyUseCase;
    private final RemoveMemberUseCase removeMemberUseCase;
    private final UpdateMemberRoleUseCase updateMemberRoleUseCase;
    private final UpdateMemberUseCase updateMemberUseCase;
    private final GrantPermissionOverrideUseCase grantPermissionOverrideUseCase;
    private final GetMemberActivityLogsUseCase getMemberActivityLogsUseCase;

    /**
     * Invite a new member to the organization by email.
     * Requires MEMBER:INVITE permission.
     *
     * POST /api/business/organization/members/invite
     */
    @PostMapping("/members/invite")
    @PreAuthorize("hasPermission('MEMBER', 'INVITE')")
    public ResponseEntity<InviteMemberResponse> inviteMember(
        @Valid @RequestBody InviteMemberRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Inviting member {} to organization {}",
            request.getEmail(), request.getOrganizationId());

        InviteMemberResponse response = inviteMemberUseCase.execute(
            request,
            userDetails.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a member directly without invitation.
     * Requires MEMBER:CREATE permission.
     *
     * POST /api/business/organization/members/create
     */
    @PostMapping("/members/create")
    @PreAuthorize("hasPermission('MEMBER', 'CREATE')")
    public ResponseEntity<UserDto> createMember(
        @Valid @RequestBody CreateMemberRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Creating member {} in organization {}",
            request.getEmail(), request.getOrganizationId());

        UserDto user = createMemberDirectlyUseCase.execute(
            request,
            userDetails.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Get all members in the organization.
     * Requires HIERARCHY:VIEW permission.
     *
     * GET /api/business/organization/{organizationId}/members
     */
    @GetMapping("/{organizationId}/members")
    @PreAuthorize("hasPermission('HIERARCHY', 'VIEW') && " +
                  "@permissionEvaluator.hasOrganizationScope(authentication, #organizationId)")
    public ResponseEntity<List<OrganizationMemberDto>> getMembers(
        @PathVariable UUID organizationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting members for organization {}", organizationId);

        List<OrganizationMemberDto> members = getOrganizationMembersUseCase.execute(organizationId);

        return ResponseEntity.ok(members);
    }

    /**
     * Get organization hierarchy tree.
     * Requires HIERARCHY:VIEW permission.
     *
     * GET /api/business/organization/{organizationId}/hierarchy
     */
    @GetMapping("/{organizationId}/hierarchy")
    @PreAuthorize("hasPermission('HIERARCHY', 'VIEW') && " +
                  "@permissionEvaluator.hasOrganizationScope(authentication, #organizationId)")
    public ResponseEntity<List<OrganizationHierarchyDto>> getHierarchy(
        @PathVariable UUID organizationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting hierarchy for organization {}", organizationId);

        List<OrganizationHierarchyDto> hierarchy = getOrganizationHierarchyUseCase.execute(organizationId);

        return ResponseEntity.ok(hierarchy);
    }

    /**
     * Update member information (position, title, manager, status).
     * Requires MEMBER:UPDATE_ROLE permission.
     *
     * PUT /api/business/organization/{organizationId}/members/{memberId}
     */
    @PutMapping("/{organizationId}/members/{memberId}")
    @PreAuthorize("hasPermission('MEMBER', 'UPDATE_ROLE')")
    public ResponseEntity<Void> updateMember(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @Valid @RequestBody UpdateMemberRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Updating member {} in organization {}", memberId, organizationId);

        updateMemberUseCase.execute(memberId, request, userDetails.getId(), organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a member from the organization.
     * Requires MEMBER:REMOVE permission.
     *
     * DELETE /api/business/organization/{organizationId}/members/{memberId}
     */
    @DeleteMapping("/{organizationId}/members/{memberId}")
    @PreAuthorize("hasPermission('MEMBER', 'REMOVE')")
    public ResponseEntity<Void> removeMember(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Removing member {} from organization {}", memberId, organizationId);

        removeMemberUseCase.execute(memberId, userDetails.getId(), organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Update member's role.
     * Requires MEMBER:UPDATE_ROLE permission.
     *
     * PUT /api/business/organization/{organizationId}/members/{memberId}/role
     */
    @PutMapping("/{organizationId}/members/{memberId}/role")
    @PreAuthorize("hasPermission('MEMBER', 'UPDATE_ROLE')")
    public ResponseEntity<Void> updateMemberRole(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @Valid @RequestBody UpdateMemberRoleRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Updating role for member {} in organization {}", memberId, organizationId);

        updateMemberRoleUseCase.execute(memberId, request, userDetails.getId(), organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Grant or revoke a custom permission to a member.
     * Requires PERMISSION:GRANT permission.
     *
     * POST /api/business/organization/permissions/grant
     */
    @PostMapping("/permissions/grant")
    @PreAuthorize("hasPermission('PERMISSION', 'GRANT')")
    public ResponseEntity<Void> grantPermission(
        @Valid @RequestBody GrantPermissionRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Granting permission {} to user {} in organization {}",
            request.getPermissionId(), request.getUserId(), request.getOrganizationId());

        grantPermissionOverrideUseCase.execute(request, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Get activity logs for the organization.
     * Requires ACTIVITY:VIEW permission.
     *
     * GET /api/business/organization/{organizationId}/activity-logs
     */
    @GetMapping("/{organizationId}/activity-logs")
    @PreAuthorize("hasPermission('ACTIVITY', 'VIEW') && " +
                  "@permissionEvaluator.hasOrganizationScope(authentication, #organizationId)")
    public ResponseEntity<Page<ActivityLogDto>> getActivityLogs(
        @PathVariable UUID organizationId,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting activity logs for organization {}", organizationId);

        ActivityLogFilterRequest filter = ActivityLogFilterRequest.builder()
            .organizationId(organizationId)
            .userId(userId)
            .action(action)
            .page(page != null ? page : 0)
            .size(size != null ? size : 20)
            .build();

        Page<ActivityLogDto> logs = getMemberActivityLogsUseCase.execute(filter);

        return ResponseEntity.ok(logs);
    }
}
