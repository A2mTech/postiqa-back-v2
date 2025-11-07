package fr.postiqa.core.domain.enums;

/**
 * Supported AI providers for analysis operations
 */
public enum AIProvider {
    OPENAI("OpenAI", "gpt-4-turbo", true, true),
    GEMINI("Google Gemini", "gemini-2.0-flash-exp", true, true);

    private final String displayName;
    private final String defaultModel;
    private final boolean supportsTextAnalysis;
    private final boolean supportsVisionAnalysis;

    AIProvider(String displayName, String defaultModel, boolean supportsTextAnalysis, boolean supportsVisionAnalysis) {
        this.displayName = displayName;
        this.defaultModel = defaultModel;
        this.supportsTextAnalysis = supportsTextAnalysis;
        this.supportsVisionAnalysis = supportsVisionAnalysis;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public boolean supportsTextAnalysis() {
        return supportsTextAnalysis;
    }

    public boolean supportsVisionAnalysis() {
        return supportsVisionAnalysis;
    }

    /**
     * Check if this provider supports a specific analysis type
     */
    public boolean supports(AnalysisType analysisType) {
        if (analysisType.requiresVisionModel()) {
            return supportsVisionAnalysis;
        }
        return supportsTextAnalysis;
    }

    /**
     * Check if this provider supports multimodal analysis
     */
    public boolean supportsMultimodal() {
        return supportsTextAnalysis && supportsVisionAnalysis;
    }
}
