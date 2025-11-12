package fr.postiqa.core.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.port.VisionAnalysisPort;
import fr.postiqa.core.infrastructure.exception.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP client for OpenAI GPT-4 Vision API.
 * <p>
 * Handles image and video frame analysis using GPT-4 Vision.
 * Supports single/multiple images, multimodal analysis, and OCR.
 */
@Component
public class GPT4VisionClient {

    private static final Logger log = LoggerFactory.getLogger(GPT4VisionClient.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String VISION_MODEL = "gpt-4-vision-preview";
    private static final int MAX_TOKENS = 4096;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;

    public GPT4VisionClient(
        @Value("${openai.api-key}") String openAiApiKey,
        ObjectMapper objectMapper
    ) {
        this.openAiApiKey = openAiApiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl(OPENAI_API_URL)
            .defaultHeader("Authorization", "Bearer " + openAiApiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /**
     * Analyze a single image with a custom prompt
     */
    public Map<String, Object> analyzeImage(String imageUrl, String prompt) {
        log.info("Analyzing single image with GPT-4 Vision");

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", imageUrl)
        ));

        return sendVisionRequest(content);
    }

    /**
     * Analyze multiple images with a custom prompt
     */
    public Map<String, Object> analyzeImages(List<String> imageUrls, String prompt) {
        log.info("Analyzing {} images with GPT-4 Vision", imageUrls.size());

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));

        for (String imageUrl : imageUrls) {
            content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", imageUrl)
            ));
        }

        return sendVisionRequest(content);
    }

    /**
     * Analyze an image with text context (multimodal)
     */
    public Map<String, Object> analyzeImageWithText(String imageUrl, String textContent, String prompt) {
        log.info("Analyzing image with text context");

        String combinedPrompt = String.format(
            "%s\n\nText content:\n%s",
            prompt,
            textContent
        );

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", combinedPrompt));
        content.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", imageUrl)
        ));

        return sendVisionRequest(content);
    }

    /**
     * Analyze a video frame with context
     */
    public Map<String, Object> analyzeVideoFrame(
        String frameUrl,
        double timestamp,
        String transcriptSegment,
        String prompt
    ) {
        log.info("Analyzing video frame at timestamp {} seconds", timestamp);

        String contextPrompt = String.format(
            "%s\n\nTimestamp: %.1f seconds\nTranscript at this moment: %s",
            prompt,
            timestamp,
            transcriptSegment != null ? transcriptSegment : "N/A"
        );

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", contextPrompt));
        content.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", frameUrl)
        ));

        return sendVisionRequest(content);
    }

    /**
     * Analyze multiple video frames
     */
    public Map<String, Object> analyzeVideoFrames(
        List<VisionAnalysisPort.VideoFrame> frames,
        String prompt
    ) {
        log.info("Analyzing {} video frames", frames.size());

        StringBuilder contextBuilder = new StringBuilder(prompt);
        contextBuilder.append("\n\nVideo frames:\n");

        for (int i = 0; i < frames.size(); i++) {
            VisionAnalysisPort.VideoFrame frame = frames.get(i);
            contextBuilder.append(String.format(
                "Frame %d - Position: %s, Timestamp: %.1fs, Transcript: %s\n",
                i + 1,
                frame.position(),
                frame.timestamp(),
                frame.transcriptSegment()
            ));
        }

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", contextBuilder.toString()));

        for (VisionAnalysisPort.VideoFrame frame : frames) {
            content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", frame.frameUrl())
            ));
        }

        return sendVisionRequest(content);
    }

    /**
     * Extract text from image using OCR
     */
    public String extractText(String imageUrl) {
        log.info("Extracting text from image (OCR)");

        String prompt = "Extract all text visible in this image. Return only the text content, preserving the order and structure as it appears. If there is no text, return an empty string.";

        Map<String, Object> result = analyzeImage(imageUrl, prompt);
        return (String) result.getOrDefault("extracted_text", "");
    }

    /**
     * Send vision analysis request to OpenAI API
     */
    private Map<String, Object> sendVisionRequest(List<Map<String, Object>> content) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", VISION_MODEL);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", content)
            ));
            requestBody.put("max_tokens", MAX_TOKENS);

            String responseStr = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ProviderUnavailableException("GPT-4 Vision",
                        "Client error: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ProviderUnavailableException("GPT-4 Vision",
                        "Server error: " + response.getStatusCode());
                })
                .body(String.class);

            return parseVisionResponse(responseStr);

        } catch (Exception e) {
            log.error("Failed to analyze image with GPT-4 Vision", e);
            throw new ProviderUnavailableException("GPT-4 Vision", "Vision analysis failed", e);
        }
    }

    /**
     * Parse GPT-4 Vision API response
     */
    private Map<String, Object> parseVisionResponse(String responseStr) throws IOException {
        JsonNode root = objectMapper.readTree(responseStr);

        String content = root.path("choices")
            .get(0)
            .path("message")
            .path("content")
            .asText();

        // Try to parse content as JSON if it looks like JSON
        Map<String, Object> result = new HashMap<>();
        try {
            if (content.trim().startsWith("{")) {
                JsonNode contentJson = objectMapper.readTree(content);
                result = objectMapper.convertValue(contentJson, Map.class);
            } else {
                result.put("analysis", content);
                result.put("raw_content", content);
            }
        } catch (Exception e) {
            // Not JSON, return as text
            result.put("analysis", content);
            result.put("raw_content", content);
        }

        // Add metadata
        result.put("model", root.path("model").asText());
        result.put("usage", objectMapper.convertValue(
            root.path("usage"),
            Map.class
        ));

        return result;
    }

    /**
     * Complete a text-only prompt using the vision model (useful for synthesizing vision analyses)
     */
    public String completeTextOnly(String systemInstruction, String userPrompt) {
        log.info("Calling GPT-4 Vision API for text-only completion");

        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(Map.of(
                    "role", "system",
                    "content", systemInstruction
                ));
            }

            messages.add(Map.of(
                "role", "user",
                "content", userPrompt
            ));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", VISION_MODEL);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", MAX_TOKENS);

            String responseStr = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ProviderUnavailableException("GPT-4 Vision",
                        "Client error: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ProviderUnavailableException("GPT-4 Vision",
                        "Server error: " + response.getStatusCode());
                })
                .body(String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        } catch (Exception e) {
            log.error("Failed to complete text with GPT-4 Vision", e);
            throw new ProviderUnavailableException("GPT-4 Vision", "Text completion failed", e);
        }
    }

    /**
     * Check if GPT-4 Vision API is available
     */
    public boolean isAvailable() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}
