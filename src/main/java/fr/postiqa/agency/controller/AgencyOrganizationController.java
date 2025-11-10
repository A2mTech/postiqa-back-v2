package fr.postiqa.agency.controller;

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
 * REST Controller for agency organization management.
 * Handles organization member management for agencies with multi-client support.
 */
@RestController
@RequestMapping("/api/agency/organization")
@RequiredArgsConstructor
@Slf4j
public class AgencyOrganizationController {

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
     * Invite a new member to the agency.
     * Supports client-scoped invitations for AGENCY_USER role.
     * Requires MEMBER:INVITE permission.
     *
     * POST /api/agency/organization/members/invite
     */
    @PostMapping("/members/invite")
    @PreAuthorize("hasPermission('MEMBER', 'INVITE')")
    public ResponseEntity<InviteMemberResponse> inviteMember(
        @Valid @RequestBody InviteMemberRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Agency inviting member {} to organization {} (client: {})",
            request.getEmail(), request.getOrganizationId(), request.getClientId());

        // Validate client scope if specified
        if (request.getClientId() != null &&
            !userDetails.getScopes().stream().anyMatch(s ->
                s.getClientId() != null && s.getClientId().equals(request.getClientId()))) {
            throw new SecurityException("You don't have access to this client");
        }

        InviteMemberResponse response = inviteMemberUseCase.execute(
            request,
            userDetails.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a member directly without invitation.
     * Supports client-scoped member creation.
     * Requires MEMBER:CREATE permission.
     *
     * POST /api/agency/organization/members/create
     */
    @PostMapping("/members/create")
    @PreAuthorize("hasPermission('MEMBER', 'CREATE')")
    public ResponseEntity<UserDto> createMember(
        @Valid @RequestBody CreateMemberRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Agency creating member {} in organization {} (client: {})",
            request.getEmail(), request.getOrganizationId(), request.getClientId());

        UserDto user = createMemberDirectlyUseCase.execute(
            request,
            userDetails.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Get all members in the agency organization.
     * Requires HIERARCHY:VIEW permission.
     *
     * GET /api/agency/organization/{organizationId}/members
     */
    @GetMapping("/{organizationId}/members")
    @PreAuthorize("hasPermission('HIERARCHY', 'VIEW') && " +
                  "@permissionEvaluator.hasOrganizationScope(authentication, #organizationId)")
    public ResponseEntity<List<OrganizationMemberDto>> getMembers(
        @PathVariable UUID organizationId,
        @RequestParam(required = false) UUID clientId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting members for agency organization {} (client filter: {})",
            organizationId, clientId);

        // TODO: Filter by clientId if specified
        List<OrganizationMemberDto> members = getOrganizationMembersUseCase.execute(organizationId);

        return ResponseEntity.ok(members);
    }

    /**
     * Get organization hierarchy tree.
     * Requires HIERARCHY:VIEW permission.
     *
     * GET /api/agency/organization/{organizationId}/hierarchy
     */
    @GetMapping("/{organizationId}/hierarchy")
    @PreAuthorize("hasPermission('HIERARCHY', 'VIEW') && " +
                  "@permissionEvaluator.hasOrganizationScope(authentication, #organizationId)")
    public ResponseEntity<List<OrganizationHierarchyDto>> getHierarchy(
        @PathVariable UUID organizationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting hierarchy for agency organization {}", organizationId);

        List<OrganizationHierarchyDto> hierarchy = getOrganizationHierarchyUseCase.execute(organizationId);

        return ResponseEntity.ok(hierarchy);
    }

    /**
     * Update member information (position, title, manager, status).
     * Requires MEMBER:UPDATE_ROLE permission.
     *
     * PUT /api/agency/organization/{organizationId}/members/{memberId}
     */
    @PutMapping("/{organizationId}/members/{memberId}")
    @PreAuthorize("hasPermission('MEMBER', 'UPDATE_ROLE')")
    public ResponseEntity<Void> updateMember(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @Valid @RequestBody UpdateMemberRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Updating agency member {} in organization {}", memberId, organizationId);

        updateMemberUseCase.execute(memberId, request, userDetails.getId(), organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a member from the agency.
     * Requires MEMBER:REMOVE permission.
     *
     * DELETE /api/agency/organization/{organizationId}/members/{memberId}
     */
    @DeleteMapping("/{organizationId}/members/{memberId}")
    @PreAuthorize("hasPermission('MEMBER', 'REMOVE')")
    public ResponseEntity<Void> removeMember(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Removing agency member {} from organization {}", memberId, organizationId);

        removeMemberUseCase.execute(memberId, userDetails.getId(), organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Update member's role and optionally change client scope.
     * Requires MEMBER:UPDATE_ROLE permission.
     *
     * PUT /api/agency/organization/{organizationId}/members/{memberId}/role
     */
    @PutMapping("/{organizationId}/members/{memberId}/role")
    @PreAuthorize("hasPermission('MEMBER', 'UPDATE_ROLE')")
    public ResponseEntity<Void> updateMemberRole(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @Valid @RequestBody UpdateMemberRoleRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Updating role for agency member {} in organization {} (new client scope: {})",
            memberId, organizationId, request.getClientId());

        updateMemberRoleUseCase.execute(memberId, request, userDetails.getId(), organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Delegate client access to a member.
     * This is specific to agencies - allows assigning a member to specific clients.
     * Requires MEMBER:UPDATE_ROLE permission.
     *
     * PUT /api/agency/organization/{organizationId}/members/{memberId}/clients
     */
    @PutMapping("/{organizationId}/members/{memberId}/clients")
    @PreAuthorize("hasPermission('MEMBER', 'UPDATE_ROLE')")
    public ResponseEntity<Void> delegateClientAccess(
        @PathVariable UUID organizationId,
        @PathVariable UUID memberId,
        @RequestBody List<UUID> clientIds,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Delegating client access for member {} in organization {}: {} clients",
            memberId, organizationId, clientIds.size());

        // TODO: Implement DelegateClientAccessUseCase
        // This would create/update UserRoleEntity entries with specific clientIds

        return ResponseEntity.noContent().build();
    }

    /**
     * Grant or revoke a custom permission to a member.
     * Requires PERMISSION:GRANT permission.
     *
     * POST /api/agency/organization/permissions/grant
     */
    @PostMapping("/permissions/grant")
    @PreAuthorize("hasPermission('PERMISSION', 'GRANT')")
    public ResponseEntity<Void> grantPermission(
        @Valid @RequestBody GrantPermissionRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Agency granting permission {} to user {} in organization {}",
            request.getPermissionId(), request.getUserId(), request.getOrganizationId());

        grantPermissionOverrideUseCase.execute(request, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Get activity logs for the agency organization.
     * Can filter by client.
     * Requires ACTIVITY:VIEW permission.
     *
     * GET /api/agency/organization/{organizationId}/activity-logs
     */
    @GetMapping("/{organizationId}/activity-logs")
    @PreAuthorize("hasPermission('ACTIVITY', 'VIEW') && " +
                  "@permissionEvaluator.hasOrganizationScope(authentication, #organizationId)")
    public ResponseEntity<Page<ActivityLogDto>> getActivityLogs(
        @PathVariable UUID organizationId,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) UUID clientId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting activity logs for agency organization {} (client filter: {})",
            organizationId, clientId);

        // Validate client scope if filtering by client
        if (clientId != null &&
            !userDetails.getScopes().stream().anyMatch(s ->
                s.getClientId() != null && s.getClientId().equals(clientId))) {
            throw new SecurityException("You don't have access to this client");
        }

        ActivityLogFilterRequest filter = ActivityLogFilterRequest.builder()
            .organizationId(organizationId)
            .clientId(clientId)
            .userId(userId)
            .action(action)
            .page(page != null ? page : 0)
            .size(size != null ? size : 20)
            .build();

        Page<ActivityLogDto> logs = getMemberActivityLogsUseCase.execute(filter);

        return ResponseEntity.ok(logs);
    }
}
