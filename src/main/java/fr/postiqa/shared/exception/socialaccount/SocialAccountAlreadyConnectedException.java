package fr.postiqa.shared.exception.socialaccount;

/**
 * Exception thrown when attempting to connect a social account that is already connected.
 */
public class SocialAccountAlreadyConnectedException extends RuntimeException {

    public SocialAccountAlreadyConnectedException(String message) {
        super(message);
    }

    public SocialAccountAlreadyConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
