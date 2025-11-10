package fr.postiqa.shared.exception.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when a token (JWT, refresh, reset password, or email verification) has expired.
 */
public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
