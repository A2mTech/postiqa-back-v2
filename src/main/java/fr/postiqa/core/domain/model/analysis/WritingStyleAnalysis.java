package fr.postiqa.core.domain.model.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive writing style analysis result
 */
public record WritingStyleAnalysis(
    String tone,
    String toneDescription,
    String narrativeStructure,
    List<String> commonHooks,
    String emojiUsage,
    String vocabulary,
    List<String> frequentPhrases,
    String punctuationStyle,
    String paragraphStructure,
    Integer averagePostLength,
    Map<String, Object> additionalMetrics
) {
    public WritingStyleAnalysis {
        commonHooks = commonHooks != null ? List.copyOf(commonHooks) : Collections.emptyList();
        frequentPhrases = frequentPhrases != null ? List.copyOf(frequentPhrases) : Collections.emptyList();
        additionalMetrics = additionalMetrics != null ? Map.copyOf(additionalMetrics) : Collections.emptyMap();
    }

    public boolean usesEmojis() {
        return emojiUsage != null && !emojiUsage.toLowerCase().contains("none") && !emojiUsage.toLowerCase().contains("pas");
    }

    public boolean hasCommonHooks() {
        return commonHooks != null && !commonHooks.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tone;
        private String toneDescription;
        private String narrativeStructure;
        private List<String> commonHooks = Collections.emptyList();
        private String emojiUsage;
        private String vocabulary;
        private List<String> frequentPhrases = Collections.emptyList();
        private String punctuationStyle;
        private String paragraphStructure;
        private Integer averagePostLength;
        private Map<String, Object> additionalMetrics = Collections.emptyMap();

        public Builder tone(String tone) {
            this.tone = tone;
            return this;
        }

        public Builder toneDescription(String toneDescription) {
            this.toneDescription = toneDescription;
            return this;
        }

        public Builder narrativeStructure(String narrativeStructure) {
            this.narrativeStructure = narrativeStructure;
            return this;
        }

        public Builder commonHooks(List<String> commonHooks) {
            this.commonHooks = commonHooks;
            return this;
        }

        public Builder emojiUsage(String emojiUsage) {
            this.emojiUsage = emojiUsage;
            return this;
        }

        public Builder vocabulary(String vocabulary) {
            this.vocabulary = vocabulary;
            return this;
        }

        public Builder frequentPhrases(List<String> frequentPhrases) {
            this.frequentPhrases = frequentPhrases;
            return this;
        }

        public Builder punctuationStyle(String punctuationStyle) {
            this.punctuationStyle = punctuationStyle;
            return this;
        }

        public Builder paragraphStructure(String paragraphStructure) {
            this.paragraphStructure = paragraphStructure;
            return this;
        }

        public Builder averagePostLength(Integer averagePostLength) {
            this.averagePostLength = averagePostLength;
            return this;
        }

        public Builder additionalMetrics(Map<String, Object> additionalMetrics) {
            this.additionalMetrics = additionalMetrics;
            return this;
        }

        public WritingStyleAnalysis build() {
            return new WritingStyleAnalysis(
                tone,
                toneDescription,
                narrativeStructure,
                commonHooks,
                emojiUsage,
                vocabulary,
                frequentPhrases,
                punctuationStyle,
                paragraphStructure,
                averagePostLength,
                additionalMetrics
            );
        }
    }
}
