package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when attempting to set an invalid manager
 * (e.g., manager not in same organization, lower role, etc.).
 */
public class InvalidManagerException extends RuntimeException {

    public InvalidManagerException(String message) {
        super(message);
    }

    public InvalidManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
