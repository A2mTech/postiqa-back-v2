package fr.postiqa.core.domain.enums;

/**
 * Types of AI analysis that can be performed on social media content
 */
public enum AnalysisType {
    // General analysis types
    WRITING_STYLE("Writing Style", "Analyze tone, narrative structure, vocabulary, and writing patterns"),
    CONTENT_THEMES("Content Themes", "Extract topics, sentiments, intentions, and key messages"),
    IMAGE_ANALYSIS("Image Analysis", "Analyze visual content, graphics, branding, and visual elements"),
    MULTIMODAL("Multimodal Analysis", "Analyze combined text and image content for coherence and impact"),

    // Ultra-deep analysis types (profile)
    PROFILE_PICTURE_ANALYSIS("Profile Picture Analysis", "Analyze profile picture for visual identity, professionalism, and branding"),
    BANNER_ANALYSIS("Banner Analysis", "Analyze profile banner for brand coherence and visual messaging"),
    BIO_ANALYSIS("Bio Analysis", "Analyze profile bio for tone, positioning, and value proposition"),

    // Ultra-deep analysis types (content)
    SITE_ANALYSIS("Site Analysis", "Analyze complete website for business identity, offerings, and brand"),
    POST_STRUCTURE_ANALYSIS("Post Structure Analysis", "Analyze individual post structure, hooks, body, and CTAs"),
    CAROUSEL_ANALYSIS("Carousel Analysis", "Analyze multi-slide carousel posts for narrative flow and engagement"),
    VIDEO_ANALYSIS("Video Analysis", "Analyze video content combining transcription and visual frames"),
    THREAD_ANALYSIS("Thread Analysis", "Analyze Twitter thread structure, progression, and coherence"),

    // Ultra-deep analysis types (aggregation)
    PLATFORM_SUMMARY("Platform Summary", "Aggregate platform-level insights and patterns"),
    CROSS_REFERENCE("Cross-Reference Analysis", "Synthesize insights across multiple platforms"),
    SCORING("Scoring & Insights", "Calculate quality scores and generate actionable recommendations");

    private final String displayName;
    private final String description;

    AnalysisType(String displayName, String description) {
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
     * Check if this analysis type requires visual/image processing
     */
    public boolean requiresVisionModel() {
        return switch (this) {
            case IMAGE_ANALYSIS, MULTIMODAL, PROFILE_PICTURE_ANALYSIS, BANNER_ANALYSIS,
                 CAROUSEL_ANALYSIS, VIDEO_ANALYSIS -> true;
            default -> false;
        };
    }

    /**
     * Check if this analysis type processes text content
     */
    public boolean processesText() {
        return switch (this) {
            case WRITING_STYLE, CONTENT_THEMES, MULTIMODAL, BIO_ANALYSIS, SITE_ANALYSIS,
                 POST_STRUCTURE_ANALYSIS, THREAD_ANALYSIS, PLATFORM_SUMMARY, CROSS_REFERENCE, SCORING -> true;
            default -> false;
        };
    }

    /**
     * Check if this analysis type requires audio transcription
     */
    public boolean requiresTranscription() {
        return this == VIDEO_ANALYSIS;
    }
}
