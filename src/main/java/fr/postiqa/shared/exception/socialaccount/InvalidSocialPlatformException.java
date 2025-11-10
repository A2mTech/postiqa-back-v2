package fr.postiqa.shared.exception.socialaccount;

/**
 * Exception thrown when an invalid or unsupported social platform is specified.
 */
public class InvalidSocialPlatformException extends RuntimeException {

    public InvalidSocialPlatformException(String message) {
        super(message);
    }

    public InvalidSocialPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
