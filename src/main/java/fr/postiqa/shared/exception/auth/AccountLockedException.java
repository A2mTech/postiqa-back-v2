package fr.postiqa.shared.exception.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when user account is locked (e.g., after too many failed login attempts).
 */
public class AccountLockedException extends AuthenticationException {

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
