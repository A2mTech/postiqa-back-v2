package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when invitation is not found.
 */
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(String message) {
        super(message);
    }

    public InvitationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
