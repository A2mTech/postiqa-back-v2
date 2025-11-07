package fr.postiqa.core.domain.model.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Image and visual content analysis result
 */
public record ImageAnalysisResult(
    String visualDescription,
    List<String> detectedObjects,
    List<String> colors,
    String composition,
    String style,
    BrandingElements brandingElements,
    String emotionalImpact,
    String targetContext,
    List<String> suggestedText,
    Map<String, Object> additionalMetrics
) {
    public ImageAnalysisResult {
        detectedObjects = detectedObjects != null ? List.copyOf(detectedObjects) : Collections.emptyList();
        colors = colors != null ? List.copyOf(colors) : Collections.emptyList();
        suggestedText = suggestedText != null ? List.copyOf(suggestedText) : Collections.emptyList();
        additionalMetrics = additionalMetrics != null ? Map.copyOf(additionalMetrics) : Collections.emptyMap();
    }

    public boolean hasBranding() {
        return brandingElements != null && brandingElements.hasBrandElements();
    }

    public boolean hasTextSuggestions() {
        return suggestedText != null && !suggestedText.isEmpty();
    }

    /**
     * Branding elements detected in the image
     */
    public record BrandingElements(
        boolean hasLogo,
        List<String> brandColors,
        String brandStyle,
        String consistency
    ) {
        public BrandingElements {
            brandColors = brandColors != null ? List.copyOf(brandColors) : Collections.emptyList();
        }

        public boolean hasBrandElements() {
            return hasLogo || (brandColors != null && !brandColors.isEmpty()) || brandStyle != null;
        }

        public static BrandingElements empty() {
            return new BrandingElements(false, Collections.emptyList(), null, null);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String visualDescription;
        private List<String> detectedObjects = Collections.emptyList();
        private List<String> colors = Collections.emptyList();
        private String composition;
        private String style;
        private BrandingElements brandingElements = BrandingElements.empty();
        private String emotionalImpact;
        private String targetContext;
        private List<String> suggestedText = Collections.emptyList();
        private Map<String, Object> additionalMetrics = Collections.emptyMap();

        public Builder visualDescription(String visualDescription) {
            this.visualDescription = visualDescription;
            return this;
        }

        public Builder detectedObjects(List<String> detectedObjects) {
            this.detectedObjects = detectedObjects;
            return this;
        }

        public Builder colors(List<String> colors) {
            this.colors = colors;
            return this;
        }

        public Builder composition(String composition) {
            this.composition = composition;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder brandingElements(BrandingElements brandingElements) {
            this.brandingElements = brandingElements;
            return this;
        }

        public Builder emotionalImpact(String emotionalImpact) {
            this.emotionalImpact = emotionalImpact;
            return this;
        }

        public Builder targetContext(String targetContext) {
            this.targetContext = targetContext;
            return this;
        }

        public Builder suggestedText(List<String> suggestedText) {
            this.suggestedText = suggestedText;
            return this;
        }

        public Builder additionalMetrics(Map<String, Object> additionalMetrics) {
            this.additionalMetrics = additionalMetrics;
            return this;
        }

        public ImageAnalysisResult build() {
            return new ImageAnalysisResult(
                visualDescription,
                detectedObjects,
                colors,
                composition,
                style,
                brandingElements,
                emotionalImpact,
                targetContext,
                suggestedText,
                additionalMetrics
            );
        }
    }
}
