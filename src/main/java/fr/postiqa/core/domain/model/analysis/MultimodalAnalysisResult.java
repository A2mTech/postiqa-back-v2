package fr.postiqa.core.domain.model.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Multimodal analysis result combining text and visual content analysis
 */
public record MultimodalAnalysisResult(
    String overallMessage,
    ContentTextAlignment textImageAlignment,
    String narrativeCoherence,
    String emotionalImpact,
    List<String> keyTakeaways,
    String audienceEngagement,
    List<String> strengths,
    List<String> improvementSuggestions,
    Map<String, Object> additionalMetrics
) {
    public MultimodalAnalysisResult {
        keyTakeaways = keyTakeaways != null ? List.copyOf(keyTakeaways) : Collections.emptyList();
        strengths = strengths != null ? List.copyOf(strengths) : Collections.emptyList();
        improvementSuggestions = improvementSuggestions != null ? List.copyOf(improvementSuggestions) : Collections.emptyList();
        additionalMetrics = additionalMetrics != null ? Map.copyOf(additionalMetrics) : Collections.emptyMap();
    }

    public boolean isWellAligned() {
        return textImageAlignment != null && textImageAlignment.alignmentScore() >= 7;
    }

    public boolean hasImprovementSuggestions() {
        return improvementSuggestions != null && !improvementSuggestions.isEmpty();
    }

    /**
     * Represents the alignment between text and visual content
     */
    public record ContentTextAlignment(
        int alignmentScore,
        String alignmentQuality,
        String explanation,
        boolean complementary,
        boolean contradictory
    ) {
        public ContentTextAlignment {
            if (alignmentScore < 0 || alignmentScore > 10) {
                throw new IllegalArgumentException("Alignment score must be between 0 and 10");
            }
        }

        public boolean isStrongAlignment() {
            return alignmentScore >= 8;
        }

        public boolean isWeakAlignment() {
            return alignmentScore <= 4;
        }

        public static ContentTextAlignment of(int score, String quality, String explanation, boolean complementary) {
            return new ContentTextAlignment(score, quality, explanation, complementary, !complementary && score < 5);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String overallMessage;
        private ContentTextAlignment textImageAlignment;
        private String narrativeCoherence;
        private String emotionalImpact;
        private List<String> keyTakeaways = Collections.emptyList();
        private String audienceEngagement;
        private List<String> strengths = Collections.emptyList();
        private List<String> improvementSuggestions = Collections.emptyList();
        private Map<String, Object> additionalMetrics = Collections.emptyMap();

        public Builder overallMessage(String overallMessage) {
            this.overallMessage = overallMessage;
            return this;
        }

        public Builder textImageAlignment(ContentTextAlignment textImageAlignment) {
            this.textImageAlignment = textImageAlignment;
            return this;
        }

        public Builder narrativeCoherence(String narrativeCoherence) {
            this.narrativeCoherence = narrativeCoherence;
            return this;
        }

        public Builder emotionalImpact(String emotionalImpact) {
            this.emotionalImpact = emotionalImpact;
            return this;
        }

        public Builder keyTakeaways(List<String> keyTakeaways) {
            this.keyTakeaways = keyTakeaways;
            return this;
        }

        public Builder audienceEngagement(String audienceEngagement) {
            this.audienceEngagement = audienceEngagement;
            return this;
        }

        public Builder strengths(List<String> strengths) {
            this.strengths = strengths;
            return this;
        }

        public Builder improvementSuggestions(List<String> improvementSuggestions) {
            this.improvementSuggestions = improvementSuggestions;
            return this;
        }

        public Builder additionalMetrics(Map<String, Object> additionalMetrics) {
            this.additionalMetrics = additionalMetrics;
            return this;
        }

        public MultimodalAnalysisResult build() {
            return new MultimodalAnalysisResult(
                overallMessage,
                textImageAlignment,
                narrativeCoherence,
                emotionalImpact,
                keyTakeaways,
                audienceEngagement,
                strengths,
                improvementSuggestions,
                additionalMetrics
            );
        }
    }
}
