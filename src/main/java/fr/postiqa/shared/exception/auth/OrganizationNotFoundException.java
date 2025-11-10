package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when organization is not found.
 */
public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(String message) {
        super(message);
    }

    public OrganizationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
