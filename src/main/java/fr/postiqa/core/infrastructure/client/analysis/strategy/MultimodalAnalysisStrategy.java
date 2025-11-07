package fr.postiqa.core.infrastructure.client.analysis.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.AnalysisResult;
import fr.postiqa.core.domain.model.analysis.MultimodalAnalysisResult;
import fr.postiqa.core.infrastructure.client.analysis.prompt.MultimodalPromptBuilder;
import fr.postiqa.core.infrastructure.client.analysis.provider.AnalysisProvider;
import fr.postiqa.core.infrastructure.client.analysis.registry.AnalysisProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy for multimodal (text + image) analysis
 */
@Component
public class MultimodalAnalysisStrategy implements AnalysisStrategy<MultimodalAnalysisResult> {

    private static final Logger log = LoggerFactory.getLogger(MultimodalAnalysisStrategy.class);

    private final MultimodalPromptBuilder promptBuilder;
    private final AnalysisProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public MultimodalAnalysisStrategy(
        MultimodalPromptBuilder promptBuilder,
        AnalysisProviderRegistry providerRegistry,
        ObjectMapper objectMapper
    ) {
        this.promptBuilder = promptBuilder;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.MULTIMODAL;
    }

    @Override
    public AnalysisResult<MultimodalAnalysisResult> analyzeSinglePost(SocialPost post, AIProvider provider) {
        log.debug("Performing multimodal analysis for single post from platform: {}", post.platform());
        Instant start = Instant.now();

        try {
            if (post.mediaUrls() == null || post.mediaUrls().isEmpty()) {
                throw new IllegalArgumentException("Post has no media for multimodal analysis");
            }

            String prompt = promptBuilder.buildForSinglePost(post);
            String systemInstruction = promptBuilder.getSystemInstruction();

            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String textContent = post.content() != null ? post.content() : "";
            String response = analysisProvider.analyzeMultimodal(
                textContent,
                post.mediaUrls(),
                prompt,
                systemInstruction
            );

            MultimodalAnalysisResult analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Multimodal analysis completed in {}ms", processingTime.toMillis());

            return AnalysisResult.success(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                AnalysisGranularity.SINGLE_POST,
                provider,
                analysis,
                LocalDateTime.now(),
                processingTime,
                analysisProvider.getLastCallMetadata()
            );

        } catch (Exception e) {
            log.error("Multimodal analysis failed: {}", e.getMessage(), e);
            Duration processingTime = Duration.between(start, Instant.now());
            return AnalysisResult.failure(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                AnalysisGranularity.SINGLE_POST,
                provider,
                e.getMessage(),
                LocalDateTime.now(),
                processingTime
            );
        }
    }

    @Override
    public AnalysisResult<MultimodalAnalysisResult> analyzeBatchPosts(List<SocialPost> posts, AIProvider provider) {
        log.debug("Performing multimodal analysis for {} posts in batch", posts.size());
        Instant start = Instant.now();

        try {
            List<SocialPost> postsWithMedia = posts.stream()
                .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
                .collect(Collectors.toList());

            if (postsWithMedia.isEmpty()) {
                throw new IllegalArgumentException("No posts with media found for multimodal analysis");
            }

            String prompt = promptBuilder.buildForBatchPosts(postsWithMedia);
            String systemInstruction = promptBuilder.getSystemInstruction();

            // For batch, we analyze the prompt-formatted content as text
            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String response = analysisProvider.complete(systemInstruction, prompt);

            MultimodalAnalysisResult analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Batch multimodal analysis completed in {}ms", processingTime.toMillis());

            return AnalysisResult.success(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                AnalysisGranularity.BATCH_POSTS,
                provider,
                analysis,
                LocalDateTime.now(),
                processingTime,
                analysisProvider.getLastCallMetadata()
            );

        } catch (Exception e) {
            log.error("Batch multimodal analysis failed: {}", e.getMessage(), e);
            Duration processingTime = Duration.between(start, Instant.now());
            return AnalysisResult.failure(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                AnalysisGranularity.BATCH_POSTS,
                provider,
                e.getMessage(),
                LocalDateTime.now(),
                processingTime
            );
        }
    }

    @Override
    public AnalysisResult<MultimodalAnalysisResult> analyzeFullProfile(List<SocialPost> posts, AIProvider provider) {
        log.debug("Performing full profile multimodal analysis for {} posts", posts.size());
        return analyzeBatchPosts(posts, provider);
    }

    private MultimodalAnalysisResult parseResponse(String response) throws Exception {
        String json = extractJson(response);
        JsonNode root = objectMapper.readTree(json);

        JsonNode alignmentNode = root.path("textImageAlignment");
        MultimodalAnalysisResult.ContentTextAlignment alignment =
            MultimodalAnalysisResult.ContentTextAlignment.of(
                alignmentNode.path("alignmentScore").asInt(5),
                alignmentNode.path("alignmentQuality").asText("moderate"),
                alignmentNode.path("explanation").asText(),
                alignmentNode.path("complementary").asBoolean(true)
            );

        return MultimodalAnalysisResult.builder()
            .overallMessage(root.path("overallMessage").asText())
            .textImageAlignment(alignment)
            .narrativeCoherence(root.path("narrativeCoherence").asText())
            .emotionalImpact(root.path("emotionalImpact").asText())
            .keyTakeaways(parseStringList(root, "keyTakeaways"))
            .audienceEngagement(root.path("audienceEngagement").asText())
            .strengths(parseStringList(root, "strengths"))
            .improvementSuggestions(parseStringList(root, "improvementSuggestions"))
            .build();
    }

    private List<String> parseStringList(JsonNode root, String fieldName) {
        List<String> result = new ArrayList<>();
        JsonNode arrayNode = root.path(fieldName);
        if (arrayNode.isArray()) {
            arrayNode.forEach(node -> result.add(node.asText()));
        }
        return result;
    }

    private String extractJson(String response) {
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}
