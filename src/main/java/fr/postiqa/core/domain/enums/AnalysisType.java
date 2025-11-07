package fr.postiqa.core.domain.enums;

/**
 * Types of AI analysis that can be performed on social media content
 */
public enum AnalysisType {
    WRITING_STYLE("Writing Style", "Analyze tone, narrative structure, vocabulary, and writing patterns"),
    CONTENT_THEMES("Content Themes", "Extract topics, sentiments, intentions, and key messages"),
    IMAGE_ANALYSIS("Image Analysis", "Analyze visual content, graphics, branding, and visual elements"),
    MULTIMODAL("Multimodal Analysis", "Analyze combined text and image content for coherence and impact");

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
        return this == IMAGE_ANALYSIS || this == MULTIMODAL;
    }

    /**
     * Check if this analysis type processes text content
     */
    public boolean processesText() {
        return this == WRITING_STYLE || this == CONTENT_THEMES || this == MULTIMODAL;
    }
}
