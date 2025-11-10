package fr.postiqa.shared.exception.auth;

import org.springframework.security.access.AccessDeniedException;

/**
 * Exception thrown when user lacks required permissions for an operation.
 */
public class InsufficientPermissionsException extends AccessDeniedException {

    public InsufficientPermissionsException(String message) {
        super(message);
    }

    public InsufficientPermissionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
