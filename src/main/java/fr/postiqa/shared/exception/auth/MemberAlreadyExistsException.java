package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when attempting to add a member that already exists in an organization.
 */
public class MemberAlreadyExistsException extends RuntimeException {

    public MemberAlreadyExistsException(String message) {
        super(message);
    }

    public MemberAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
