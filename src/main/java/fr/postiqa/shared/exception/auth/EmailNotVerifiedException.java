package fr.postiqa.shared.exception.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when user attempts to login but email is not verified.
 */
public class EmailNotVerifiedException extends AuthenticationException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }

    public EmailNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
