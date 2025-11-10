package fr.postiqa.shared.exception.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when an API key is invalid, revoked, or expired.
 */
public class ApiKeyInvalidException extends AuthenticationException {

    public ApiKeyInvalidException(String message) {
        super(message);
    }

    public ApiKeyInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
