package fr.postiqa.shared.enums;

/**
 * Enum representing social media platforms supported by the application.
 */
public enum SocialPlatform {
    LINKEDIN("LinkedIn"),
    TWITTER("Twitter/X"),
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
    TIKTOK("TikTok");

    private final String displayName;

    SocialPlatform(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
