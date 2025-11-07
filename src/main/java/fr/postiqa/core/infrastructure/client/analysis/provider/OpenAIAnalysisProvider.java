package fr.postiqa.core.infrastructure.client.analysis.provider;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.infrastructure.config.OpenAIProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI implementation of AnalysisProvider using Spring AI ChatClient
 */
@Component
public class OpenAIAnalysisProvider implements AnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIAnalysisProvider.class);

    private final ChatClient chatClient;
    private final OpenAIProperties properties;
    private final Map<String, Object> lastCallMetadata = new HashMap<>();

    public OpenAIAnalysisProvider(ChatClient.Builder chatClientBuilder, OpenAIProperties properties) {
        this.properties = properties;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public AIProvider getProviderType() {
        return AIProvider.OPENAI;
    }

    @Override
    public String complete(String systemInstruction, String userPrompt) {
        Instant start = Instant.now();
        log.debug("OpenAI completion request - prompt length: {} chars", userPrompt.length());

        try {
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                requestSpec = requestSpec.system(systemInstruction);
            }

            ChatClient.CallResponseSpec callResponse = requestSpec.user(userPrompt).call();

            // Get both content and ChatResponse for metadata
            String content = callResponse.content();
            ChatResponse response = callResponse.chatResponse();

            Duration duration = Duration.between(start, Instant.now());
            updateMetadata(response, duration);

            log.debug("OpenAI completion successful - response length: {} chars, duration: {}ms",
                content.length(), duration.toMillis());

            return content;

        } catch (Exception e) {
            log.error("OpenAI completion failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String analyzeImage(String imageUrl, String prompt, String systemInstruction) {
        log.warn("Image analysis not yet fully implemented - falling back to text-only analysis");
        // TODO: Implement with Spring AI's proper Media/UserMessage API once available
        String combinedPrompt = String.format("Analyze this image (URL: %s): %s", imageUrl, prompt);
        return complete(systemInstruction, combinedPrompt);
    }

    @Override
    public String analyzeImages(List<String> imageUrls, String prompt, String systemInstruction) {
        log.warn("Multiple images analysis not yet fully implemented - falling back to text-only analysis");
        // TODO: Implement with Spring AI's proper Media/UserMessage API once available
        String combinedPrompt = String.format("Analyze these %d images: %s", imageUrls.size(), prompt);
        return complete(systemInstruction, combinedPrompt);
    }

    @Override
    public String analyzeMultimodal(String textContent, List<String> imageUrls, String prompt, String systemInstruction) {
        log.warn("Multimodal analysis not yet fully implemented - falling back to text-only analysis");
        // TODO: Implement with Spring AI's proper Media/UserMessage API once available
        String combinedPrompt = String.format("%s\n\nText content to analyze:\n%s\n\nNumber of images: %d",
            prompt, textContent, imageUrls.size());
        return complete(systemInstruction, combinedPrompt);
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public boolean supportsMultimodal() {
        return true;
    }

    @Override
    public String getModelName() {
        return properties.getChat().getModel();
    }

    @Override
    public Map<String, Object> getLastCallMetadata() {
        return new HashMap<>(lastCallMetadata);
    }

    private void updateMetadata(ChatResponse response, Duration duration) {
        lastCallMetadata.clear();
        lastCallMetadata.put("model", getModelName());
        lastCallMetadata.put("durationMs", duration.toMillis());

        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            var usage = response.getMetadata().getUsage();
            if (usage.getPromptTokens() != null) {
                lastCallMetadata.put("promptTokens", usage.getPromptTokens());
            }
            if (usage.getTotalTokens() != null) {
                lastCallMetadata.put("totalTokens", usage.getTotalTokens());
            }
            // Calculate output tokens from total and prompt tokens
            try {
                long outputTokens = (long) usage.getTotalTokens() - (long) usage.getPromptTokens();
                lastCallMetadata.put("completionTokens", outputTokens);
            } catch (Exception e) {
                log.debug("Could not calculate completion tokens: {}", e.getMessage());
            }
        }
    }
}
