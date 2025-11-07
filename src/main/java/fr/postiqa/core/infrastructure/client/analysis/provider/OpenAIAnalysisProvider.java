package fr.postiqa.core.infrastructure.client.analysis.provider;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.infrastructure.config.OpenAIProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI implementation of AnalysisProvider using Spring AI
 */
@Component
public class OpenAIAnalysisProvider implements AnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIAnalysisProvider.class);

    private final ChatClient chatClient;
    private final OpenAIProperties properties;
    private final Map<String, Object> lastCallMetadata = new HashMap<>();

    public OpenAIAnalysisProvider(ChatClient.Builder chatClientBuilder, OpenAIProperties properties) {
        this.properties = properties;
        this.chatClient = chatClientBuilder
            .defaultOptions(builder -> builder
                .model(properties.getChat().getModel())
                .temperature(properties.getChat().getTemperature().floatValue())
                .maxTokens(properties.getChat().getMaxTokens())
            )
            .build();
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
            List<Message> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(new SystemMessage(systemInstruction));
            }
            messages.add(new UserMessage(userPrompt));

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            Duration duration = Duration.between(start, Instant.now());
            updateMetadata(response, duration);

            String content = response.getResult().getOutput().getContent();
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
        Instant start = Instant.now();
        log.debug("OpenAI image analysis request - image: {}", imageUrl);

        try {
            List<Message> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(new SystemMessage(systemInstruction));
            }

            // Create user message with image
            UserMessage userMessage = new UserMessage(
                prompt,
                List.of(new Media(MimeTypeUtils.IMAGE_PNG, new URL(imageUrl)))
            );
            messages.add(userMessage);

            Prompt fullPrompt = new Prompt(messages);
            ChatResponse response = chatClient.prompt(fullPrompt).call().chatResponse();

            Duration duration = Duration.between(start, Instant.now());
            updateMetadata(response, duration);

            String content = response.getResult().getOutput().getContent();
            log.debug("OpenAI image analysis successful - duration: {}ms", duration.toMillis());

            return content;

        } catch (Exception e) {
            log.error("OpenAI image analysis failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI image analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String analyzeImages(List<String> imageUrls, String prompt, String systemInstruction) {
        Instant start = Instant.now();
        log.debug("OpenAI multiple images analysis request - count: {}", imageUrls.size());

        try {
            List<Message> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(new SystemMessage(systemInstruction));
            }

            // Create media list for all images
            List<Media> mediaList = new ArrayList<>();
            for (String imageUrl : imageUrls) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URL(imageUrl)));
            }

            UserMessage userMessage = new UserMessage(prompt, mediaList);
            messages.add(userMessage);

            Prompt fullPrompt = new Prompt(messages);
            ChatResponse response = chatClient.prompt(fullPrompt).call().chatResponse();

            Duration duration = Duration.between(start, Instant.now());
            updateMetadata(response, duration);

            String content = response.getResult().getOutput().getContent();
            log.debug("OpenAI multiple images analysis successful - duration: {}ms", duration.toMillis());

            return content;

        } catch (Exception e) {
            log.error("OpenAI multiple images analysis failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI multiple images analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String analyzeMultimodal(String textContent, List<String> imageUrls, String prompt, String systemInstruction) {
        Instant start = Instant.now();
        log.debug("OpenAI multimodal analysis request - images: {}, text length: {}",
            imageUrls.size(), textContent.length());

        try {
            List<Message> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(new SystemMessage(systemInstruction));
            }

            // Create media list for all images
            List<Media> mediaList = new ArrayList<>();
            for (String imageUrl : imageUrls) {
                mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URL(imageUrl)));
            }

            // Combine text content with analysis prompt
            String fullPrompt = String.format("%s\n\nText content to analyze:\n%s", prompt, textContent);

            UserMessage userMessage = new UserMessage(fullPrompt, mediaList);
            messages.add(userMessage);

            Prompt fullPromptObj = new Prompt(messages);
            ChatResponse response = chatClient.prompt(fullPromptObj).call().chatResponse();

            Duration duration = Duration.between(start, Instant.now());
            updateMetadata(response, duration);

            String content = response.getResult().getOutput().getContent();
            log.debug("OpenAI multimodal analysis successful - duration: {}ms", duration.toMillis());

            return content;

        } catch (Exception e) {
            log.error("OpenAI multimodal analysis failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI multimodal analysis failed: " + e.getMessage(), e);
        }
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

        if (response.getMetadata() != null) {
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                lastCallMetadata.put("promptTokens", usage.getPromptTokens());
                lastCallMetadata.put("completionTokens", usage.getGenerationTokens());
                lastCallMetadata.put("totalTokens", usage.getTotalTokens());
            }
        }
    }
}
