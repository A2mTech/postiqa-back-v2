package fr.postiqa.core.domain.model.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Content themes and topics analysis result
 */
public record ContentThemesAnalysis(
    List<String> mainThemes,
    List<String> secondaryThemes,
    String overallSentiment,
    Map<String, String> themeSentiments,
    List<String> keyMessages,
    List<String> callsToAction,
    String contentIntent,
    String targetAudience,
    List<String> expertiseAreas,
    Map<String, Integer> themeFrequency,
    Map<String, Object> additionalMetrics
) {
    public ContentThemesAnalysis {
        mainThemes = mainThemes != null ? List.copyOf(mainThemes) : Collections.emptyList();
        secondaryThemes = secondaryThemes != null ? List.copyOf(secondaryThemes) : Collections.emptyList();
        themeSentiments = themeSentiments != null ? Map.copyOf(themeSentiments) : Collections.emptyMap();
        keyMessages = keyMessages != null ? List.copyOf(keyMessages) : Collections.emptyList();
        callsToAction = callsToAction != null ? List.copyOf(callsToAction) : Collections.emptyList();
        expertiseAreas = expertiseAreas != null ? List.copyOf(expertiseAreas) : Collections.emptyList();
        themeFrequency = themeFrequency != null ? Map.copyOf(themeFrequency) : Collections.emptyMap();
        additionalMetrics = additionalMetrics != null ? Map.copyOf(additionalMetrics) : Collections.emptyMap();
    }

    public boolean hasCallsToAction() {
        return callsToAction != null && !callsToAction.isEmpty();
    }

    public boolean isPositiveSentiment() {
        return overallSentiment != null &&
               (overallSentiment.toLowerCase().contains("positive") ||
                overallSentiment.toLowerCase().contains("positif"));
    }

    public String getMostFrequentTheme() {
        if (themeFrequency == null || themeFrequency.isEmpty()) {
            return null;
        }
        return themeFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> mainThemes = Collections.emptyList();
        private List<String> secondaryThemes = Collections.emptyList();
        private String overallSentiment;
        private Map<String, String> themeSentiments = Collections.emptyMap();
        private List<String> keyMessages = Collections.emptyList();
        private List<String> callsToAction = Collections.emptyList();
        private String contentIntent;
        private String targetAudience;
        private List<String> expertiseAreas = Collections.emptyList();
        private Map<String, Integer> themeFrequency = Collections.emptyMap();
        private Map<String, Object> additionalMetrics = Collections.emptyMap();

        public Builder mainThemes(List<String> mainThemes) {
            this.mainThemes = mainThemes;
            return this;
        }

        public Builder secondaryThemes(List<String> secondaryThemes) {
            this.secondaryThemes = secondaryThemes;
            return this;
        }

        public Builder overallSentiment(String overallSentiment) {
            this.overallSentiment = overallSentiment;
            return this;
        }

        public Builder themeSentiments(Map<String, String> themeSentiments) {
            this.themeSentiments = themeSentiments;
            return this;
        }

        public Builder keyMessages(List<String> keyMessages) {
            this.keyMessages = keyMessages;
            return this;
        }

        public Builder callsToAction(List<String> callsToAction) {
            this.callsToAction = callsToAction;
            return this;
        }

        public Builder contentIntent(String contentIntent) {
            this.contentIntent = contentIntent;
            return this;
        }

        public Builder targetAudience(String targetAudience) {
            this.targetAudience = targetAudience;
            return this;
        }

        public Builder expertiseAreas(List<String> expertiseAreas) {
            this.expertiseAreas = expertiseAreas;
            return this;
        }

        public Builder themeFrequency(Map<String, Integer> themeFrequency) {
            this.themeFrequency = themeFrequency;
            return this;
        }

        public Builder additionalMetrics(Map<String, Object> additionalMetrics) {
            this.additionalMetrics = additionalMetrics;
            return this;
        }

        public ContentThemesAnalysis build() {
            return new ContentThemesAnalysis(
                mainThemes,
                secondaryThemes,
                overallSentiment,
                themeSentiments,
                keyMessages,
                callsToAction,
                contentIntent,
                targetAudience,
                expertiseAreas,
                themeFrequency,
                additionalMetrics
            );
        }
    }
}
