package fr.postiqa.shared.exception.socialaccount;

/**
 * Exception thrown when a social account is not found by ID or criteria.
 */
public class SocialAccountNotFoundException extends RuntimeException {

    public SocialAccountNotFoundException(String message) {
        super(message);
    }

    public SocialAccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
