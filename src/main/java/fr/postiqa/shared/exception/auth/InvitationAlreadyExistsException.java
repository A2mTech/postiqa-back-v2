package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when a pending invitation already exists for the email.
 */
public class InvitationAlreadyExistsException extends RuntimeException {

    public InvitationAlreadyExistsException(String message) {
        super(message);
    }

    public InvitationAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
