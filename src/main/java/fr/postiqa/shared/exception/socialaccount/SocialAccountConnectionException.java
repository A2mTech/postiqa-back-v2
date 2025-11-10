package fr.postiqa.shared.exception.socialaccount;

/**
 * General exception for social account connection errors.
 */
public class SocialAccountConnectionException extends RuntimeException {

    public SocialAccountConnectionException(String message) {
        super(message);
    }

    public SocialAccountConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
