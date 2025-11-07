package fr.postiqa.core.infrastructure.client.analysis.provider;

import fr.postiqa.core.domain.enums.AIProvider;

import java.util.List;
import java.util.Map;

/**
 * Interface for AI provider implementations
 * Abstracts communication with different AI APIs (OpenAI, Gemini, etc.)
 */
public interface AnalysisProvider {

    /**
     * Get the provider type this implementation supports
     *
     * @return AIProvider enum value
     */
    AIProvider getProviderType();

    /**
     * Complete a text-only prompt
     *
     * @param systemInstruction System-level instruction (optional)
     * @param userPrompt User prompt to complete
     * @return AI response as JSON string
     */
    String complete(String systemInstruction, String userPrompt);

    /**
     * Complete a text-only prompt without system instruction
     *
     * @param userPrompt User prompt to complete
     * @return AI response as JSON string
     */
    default String complete(String userPrompt) {
        return complete(null, userPrompt);
    }

    /**
     * Analyze a single image with a text prompt
     *
     * @param imageUrl URL of the image to analyze
     * @param prompt Text prompt describing what to analyze
     * @param systemInstruction System-level instruction (optional)
     * @return AI response as JSON string
     */
    String analyzeImage(String imageUrl, String prompt, String systemInstruction);

    /**
     * Analyze a single image with a text prompt (no system instruction)
     *
     * @param imageUrl URL of the image to analyze
     * @param prompt Text prompt describing what to analyze
     * @return AI response as JSON string
     */
    default String analyzeImage(String imageUrl, String prompt) {
        return analyzeImage(imageUrl, prompt, null);
    }

    /**
     * Analyze multiple images with a text prompt
     *
     * @param imageUrls URLs of images to analyze
     * @param prompt Text prompt describing what to analyze
     * @param systemInstruction System-level instruction (optional)
     * @return AI response as JSON string
     */
    String analyzeImages(List<String> imageUrls, String prompt, String systemInstruction);

    /**
     * Analyze multiple images with a text prompt (no system instruction)
     *
     * @param imageUrls URLs of images to analyze
     * @param prompt Text prompt describing what to analyze
     * @return AI response as JSON string
     */
    default String analyzeImages(List<String> imageUrls, String prompt) {
        return analyzeImages(imageUrls, prompt, null);
    }

    /**
     * Analyze multimodal content (text + images combined)
     *
     * @param textContent Text content to analyze
     * @param imageUrls Image URLs to analyze alongside text
     * @param prompt Analysis prompt
     * @param systemInstruction System-level instruction (optional)
     * @return AI response as JSON string
     */
    String analyzeMultimodal(String textContent, List<String> imageUrls, String prompt, String systemInstruction);

    /**
     * Analyze multimodal content (no system instruction)
     *
     * @param textContent Text content to analyze
     * @param imageUrls Image URLs to analyze alongside text
     * @param prompt Analysis prompt
     * @return AI response as JSON string
     */
    default String analyzeMultimodal(String textContent, List<String> imageUrls, String prompt) {
        return analyzeMultimodal(textContent, imageUrls, prompt, null);
    }

    /**
     * Check if this provider supports vision/image analysis
     *
     * @return true if vision is supported
     */
    boolean supportsVision();

    /**
     * Check if this provider supports multimodal analysis
     *
     * @return true if multimodal is supported
     */
    boolean supportsMultimodal();

    /**
     * Get the model name being used by this provider
     *
     * @return Model name (e.g., "gpt-4-turbo", "gemini-2.0-flash-exp")
     */
    String getModelName();

    /**
     * Get metadata about the last API call (tokens used, latency, etc.)
     *
     * @return Metadata map
     */
    Map<String, Object> getLastCallMetadata();
}
