package fr.postiqa.core.infrastructure.exception;

import fr.postiqa.core.domain.enums.SocialPlatform;

/**
 * Exception thrown when an unsupported or invalid platform is specified
 */
public class InvalidPlatformException extends ScrapingException {

    private final SocialPlatform platform;

    public InvalidPlatformException(SocialPlatform platform) {
        super(String.format("Platform '%s' is not supported or configured", platform));
        this.platform = platform;
    }

    public InvalidPlatformException(SocialPlatform platform, String message) {
        super(String.format("Platform '%s': %s", platform, message));
        this.platform = platform;
    }

    public SocialPlatform getPlatform() {
        return platform;
    }
}
