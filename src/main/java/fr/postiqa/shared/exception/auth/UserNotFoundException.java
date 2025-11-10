package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when user is not found by email or ID.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
