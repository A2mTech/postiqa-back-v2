package fr.postiqa.shared.exception.socialaccount;

/**
 * Exception thrown when OAuth2 token refresh fails.
 */
public class OAuth2TokenRefreshException extends RuntimeException {

    public OAuth2TokenRefreshException(String message) {
        super(message);
    }

    public OAuth2TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
