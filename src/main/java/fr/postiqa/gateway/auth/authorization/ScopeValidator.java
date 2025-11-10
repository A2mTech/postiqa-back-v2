package fr.postiqa.gateway.auth.authorization;

import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.shared.exception.auth.TenantAccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Scope Validator for horizontal access control.
 * Validates that users can only access resources within their organization/client scope.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScopeValidator {

    /**
     * Validate that user has access to the specified organization
     *
     * @param userDetails current user details
     * @param organizationId target organization ID
     * @return true if user has access
     */
    public boolean hasOrganizationAccess(CustomUserDetails userDetails, UUID organizationId) {
        if (organizationId == null) {
            return false;
        }

        boolean hasAccess = userDetails.getScopes().stream()
            .anyMatch(scope -> organizationId.equals(scope.getOrganizationId()));

        if (!hasAccess) {
            log.warn("User {} attempted to access organization {} without permission",
                userDetails.getEmail(), organizationId);
        }

        return hasAccess;
    }

    /**
     * Validate that user has access to the specified client (agency mode)
     *
     * @param userDetails current user details
     * @param clientId target client ID
     * @return true if user has access
     */
    public boolean hasClientAccess(CustomUserDetails userDetails, UUID clientId) {
        if (clientId == null) {
            return false;
        }

        boolean hasAccess = userDetails.getScopes().stream()
            .anyMatch(scope -> clientId.equals(scope.getClientId()));

        if (!hasAccess) {
            log.warn("User {} attempted to access client {} without permission",
                userDetails.getEmail(), clientId);
        }

        return hasAccess;
    }

    /**
     * Validate organization access and throw exception if denied
     */
    public void requireOrganizationAccess(CustomUserDetails userDetails, UUID organizationId) {
        if (!hasOrganizationAccess(userDetails, organizationId)) {
            throw new TenantAccessDeniedException(
                "Access denied to organization: " + organizationId);
        }
    }

    /**
     * Validate client access and throw exception if denied
     */
    public void requireClientAccess(CustomUserDetails userDetails, UUID clientId) {
        if (!hasClientAccess(userDetails, clientId)) {
            throw new TenantAccessDeniedException(
                "Access denied to client: " + clientId);
        }
    }
}
