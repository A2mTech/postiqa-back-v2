package fr.postiqa.core.domain.enums;

/**
 * Supported social media platforms for scraping
 */
public enum SocialPlatform {
    LINKEDIN("LinkedIn", "linkedin.com"),
    TWITTER("Twitter/X", "twitter.com"),
    INSTAGRAM("Instagram", "instagram.com"),
    TIKTOK("TikTok", "tiktok.com"),
    YOUTUBE("YouTube", "youtube.com");

    private final String displayName;
    private final String domain;

    SocialPlatform(String displayName, String domain) {
        this.displayName = displayName;
        this.domain = domain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDomain() {
        return domain;
    }

    /**
     * Check if this platform supports video content
     */
    public boolean supportsVideo() {
        return this == TIKTOK || this == YOUTUBE || this == INSTAGRAM;
    }

    /**
     * Check if this platform is primarily professional/business focused
     */
    public boolean isProfessional() {
        return this == LINKEDIN;
    }
}
