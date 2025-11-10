package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when invitation has expired.
 */
public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException(String message) {
        super(message);
    }

    public InvitationExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
