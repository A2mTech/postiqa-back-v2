package fr.postiqa.gateway.auth.authorization;

import fr.postiqa.database.entity.UserPermissionOverrideEntity;
import fr.postiqa.database.repository.UserPermissionOverrideRepository;
import fr.postiqa.gateway.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom Permission Evaluator for @PreAuthorize expressions.
 * Evaluates resource-action permissions and scope-based access control.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final ScopeValidator scopeValidator;
    private final HierarchyValidator hierarchyValidator;
    private final UserPermissionOverrideRepository permissionOverrideRepository;

    /**
     * Evaluate permission for a resource-action pair
     * Example: hasPermission('POST', 'CREATE')
     *
     * Checks in order:
     * 1. Custom permission overrides (granted/revoked)
     * 2. Role-based permissions (exact match)
     * 3. Role-based wildcard permissions (resource:*)
     *
     * @param authentication current authentication
     * @param targetDomainObject target resource (e.g., 'POST', 'CALENDAR')
     * @param permission required action (e.g., 'CREATE', 'READ', 'UPDATE', 'DELETE')
     * @return true if user has permission
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        String resource = targetDomainObject.toString();
        String action = permission.toString();
        String requiredPermission = resource + ":" + action;

        // Check custom permission overrides first
        Boolean customPermission = checkCustomPermissionOverride(userDetails, resource, action);
        if (customPermission != null) {
            if (customPermission) {
                log.debug("User {} granted permission via override: {}", authentication.getName(), requiredPermission);
                return true;
            } else {
                log.debug("User {} denied permission via override: {}", authentication.getName(), requiredPermission);
                return false;
            }
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Check exact permission match
        boolean hasExactPermission = authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals(requiredPermission));

        if (hasExactPermission) {
            log.debug("User {} has exact permission: {}", authentication.getName(), requiredPermission);
            return true;
        }

        // Check wildcard permission (resource:*)
        String wildcardPermission = resource + ":*";
        boolean hasWildcardPermission = authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals(wildcardPermission));

        if (hasWildcardPermission) {
            log.debug("User {} has wildcard permission: {}", authentication.getName(), wildcardPermission);
            return true;
        }

        log.debug("User {} denied permission: {}", authentication.getName(), requiredPermission);
        return false;
    }

    /**
     * Evaluate permission for a specific entity with ID
     * Example: hasPermission(postId, 'POST', 'UPDATE')
     *
     * @param authentication current authentication
     * @param targetId target entity ID
     * @param targetType target entity type (e.g., 'POST', 'CALENDAR')
     * @param permission required action
     * @return true if user has permission
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // For now, delegate to the simpler method
        // In the future, this could check ownership or entity-specific permissions
        return hasPermission(authentication, targetType, permission);
    }

    /**
     * Check if user has access to specific organization
     * Used in custom SpEL expressions: @permissionEvaluator.hasOrganizationScope(#organizationId)
     *
     * @param authentication current authentication
     * @param organizationId target organization ID
     * @return true if user has access
     */
    public boolean hasOrganizationScope(Authentication authentication, UUID organizationId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return scopeValidator.hasOrganizationAccess(userDetails, organizationId);
    }

    /**
     * Check if user has access to specific client (agency mode)
     * Used in custom SpEL expressions: @permissionEvaluator.hasClientScope(#clientId)
     *
     * @param authentication current authentication
     * @param clientId target client ID
     * @return true if user has access
     */
    public boolean hasClientScope(Authentication authentication, UUID clientId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return scopeValidator.hasClientAccess(userDetails, clientId);
    }

    /**
     * Check if user can manage another member (via hierarchy).
     * A user can manage a member if they are a direct or indirect manager.
     *
     * @param authentication current authentication
     * @param targetMemberId target member user ID
     * @param organizationId organization context
     * @return true if user can manage the member
     */
    public boolean canManageMember(Authentication authentication, UUID targetMemberId, UUID organizationId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return hierarchyValidator.canManageMember(userDetails.getId(), targetMemberId, organizationId);
    }

    /**
     * Check custom permission override for a user.
     * Returns:
     * - true if permission is explicitly granted
     * - false if permission is explicitly revoked
     * - null if no override exists (fall back to role-based permissions)
     *
     * @param userDetails current user
     * @param resource target resource
     * @param action target action
     * @return Boolean (true/false) or null if no override
     */
    private Boolean checkCustomPermissionOverride(CustomUserDetails userDetails, String resource, String action) {
        // Get all organization IDs for this user
        List<UUID> organizationIds = userDetails.getScopes().stream()
            .map(CustomUserDetails.ScopeInfo::getOrganizationId)
            .filter(orgId -> orgId != null)
            .distinct()
            .toList();

        if (organizationIds.isEmpty()) {
            return null;
        }

        // Check overrides for each organization
        for (UUID organizationId : organizationIds) {
            List<UserPermissionOverrideEntity> overrides =
                permissionOverrideRepository.findByUserIdAndOrganizationIdWithPermission(
                    userDetails.getId(),
                    organizationId
                );

            for (UserPermissionOverrideEntity override : overrides) {
                String overrideResource = override.getPermission().getResource();
                String overrideAction = override.getPermission().getAction();

                // Check exact match or wildcard
                if (overrideResource.equals(resource) &&
                    (overrideAction.equals(action) || overrideAction.equals("*"))) {
                    return override.getGranted();
                }
            }
        }

        return null; // No override found
    }
}
