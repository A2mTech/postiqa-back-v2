package fr.postiqa.shared.exception.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when user provides invalid credentials (wrong email or password).
 */
public class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
