package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when organization member is not found.
 */
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(String message) {
        super(message);
    }

    public MemberNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
