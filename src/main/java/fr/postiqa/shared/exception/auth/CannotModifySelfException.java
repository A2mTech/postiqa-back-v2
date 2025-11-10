package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when a user attempts to modify their own role or permissions.
 */
public class CannotModifySelfException extends RuntimeException {

    public CannotModifySelfException(String message) {
        super(message);
    }

    public CannotModifySelfException(String message, Throwable cause) {
        super(message, cause);
    }
}
