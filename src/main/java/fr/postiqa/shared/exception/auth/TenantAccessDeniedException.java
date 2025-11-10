package fr.postiqa.shared.exception.auth;

import org.springframework.security.access.AccessDeniedException;

/**
 * Exception thrown when user attempts to access resources from another tenant (agency multi-tenant).
 */
public class TenantAccessDeniedException extends AccessDeniedException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
