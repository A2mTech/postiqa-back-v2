package fr.postiqa.core.domain.enums;

/**
 * Granularity levels for analysis operations
 */
public enum AnalysisGranularity {
    SINGLE_POST("Single Post", "Analyze a single social media post in isolation"),
    BATCH_POSTS("Batch Posts", "Analyze multiple posts together to identify patterns"),
    FULL_PROFILE("Full Profile", "Comprehensive analysis of entire user profile across platforms");

    private final String displayName;
    private final String description;

    AnalysisGranularity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this granularity requires multiple data points
     */
    public boolean requiresMultiplePosts() {
        return this == BATCH_POSTS || this == FULL_PROFILE;
    }

    /**
     * Check if this granularity requires cross-platform aggregation
     */
    public boolean requiresCrossPlatform() {
        return this == FULL_PROFILE;
    }

    /**
     * Get recommended minimum number of posts for this granularity
     */
    public int getRecommendedMinPosts() {
        return switch (this) {
            case SINGLE_POST -> 1;
            case BATCH_POSTS -> 10;
            case FULL_PROFILE -> 30;
        };
    }
}
