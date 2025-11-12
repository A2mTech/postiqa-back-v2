package fr.postiqa.core.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * HTTP client for OpenAI Chat Completions API (text-only, non-vision models).
 * <p>
 * Handles text generation, analysis, and synthesis using GPT-4 and GPT-3.5.
 * For image analysis, use {@link GPT4VisionClient} instead.
 */
@Component
public class OpenAIClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4-turbo-preview";
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;

    public OpenAIClient(
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
     * Complete a prompt with system instruction (default model and settings)
     */
    public String complete(String systemInstruction, String userPrompt) {
        return complete(systemInstruction, userPrompt, DEFAULT_MODEL, DEFAULT_MAX_TOKENS);
    }

    /**
     * Complete a prompt with custom model and max tokens
     */
    public String complete(String systemInstruction, String userPrompt, String model, int maxTokens) {
        log.info("Calling OpenAI API with model: {}", model);

        try {
            List<Map<String, String>> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemInstruction));
            }

            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);

            String responseStr = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ProviderUnavailableException("OpenAI",
                        "Client error: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ProviderUnavailableException("OpenAI",
                        "Server error: " + response.getStatusCode());
                })
                .body(String.class);

            return parseCompletionResponse(responseStr);

        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            throw new ProviderUnavailableException("OpenAI", "Completion failed", e);
        }
    }

    /**
     * Complete a prompt with just user message (no system instruction)
     */
    public String complete(String userPrompt) {
        return complete(null, userPrompt);
    }

    /**
     * Parse OpenAI Chat Completions API response
     */
    private String parseCompletionResponse(String responseStr) throws IOException {
        JsonNode root = objectMapper.readTree(responseStr);

        return root.path("choices")
            .get(0)
            .path("message")
            .path("content")
            .asText();
    }

    /**
     * Check if OpenAI API is available
     */
    public boolean isAvailable() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}
