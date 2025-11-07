package fr.postiqa.core.infrastructure.client.analysis.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.AnalysisResult;
import fr.postiqa.core.domain.model.analysis.WritingStyleAnalysis;
import fr.postiqa.core.infrastructure.client.analysis.prompt.WritingStylePromptBuilder;
import fr.postiqa.core.infrastructure.client.analysis.provider.AnalysisProvider;
import fr.postiqa.core.infrastructure.client.analysis.registry.AnalysisProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Strategy for analyzing writing style
 */
@Component
public class WritingStyleAnalysisStrategy implements AnalysisStrategy<WritingStyleAnalysis> {

    private static final Logger log = LoggerFactory.getLogger(WritingStyleAnalysisStrategy.class);

    private final WritingStylePromptBuilder promptBuilder;
    private final AnalysisProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public WritingStyleAnalysisStrategy(
        WritingStylePromptBuilder promptBuilder,
        AnalysisProviderRegistry providerRegistry,
        ObjectMapper objectMapper
    ) {
        this.promptBuilder = promptBuilder;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.WRITING_STYLE;
    }

    @Override
    public AnalysisResult<WritingStyleAnalysis> analyzeSinglePost(SocialPost post, AIProvider provider) {
        log.debug("Analyzing writing style for single post from platform: {}", post.platform());
        Instant start = Instant.now();

        try {
            // Build prompt
            String prompt = promptBuilder.buildForSinglePost(post);
            String systemInstruction = promptBuilder.getSystemInstruction();

            // Get provider and execute
            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String response = analysisProvider.complete(systemInstruction, prompt);

            // Parse response
            WritingStyleAnalysis analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Writing style analysis completed in {}ms", processingTime.toMillis());

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
            log.error("Writing style analysis failed: {}", e.getMessage(), e);
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
    public AnalysisResult<WritingStyleAnalysis> analyzeBatchPosts(List<SocialPost> posts, AIProvider provider) {
        log.debug("Analyzing writing style for {} posts in batch", posts.size());
        Instant start = Instant.now();

        try {
            String prompt = promptBuilder.buildForBatchPosts(posts);
            String systemInstruction = promptBuilder.getSystemInstruction();

            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String response = analysisProvider.complete(systemInstruction, prompt);

            WritingStyleAnalysis analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Batch writing style analysis completed in {}ms", processingTime.toMillis());

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
            log.error("Batch writing style analysis failed: {}", e.getMessage(), e);
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
    public AnalysisResult<WritingStyleAnalysis> analyzeFullProfile(List<SocialPost> posts, AIProvider provider) {
        log.debug("Analyzing full writing style profile for {} posts", posts.size());
        Instant start = Instant.now();

        try {
            String prompt = promptBuilder.buildForFullProfile(posts);
            String systemInstruction = promptBuilder.getSystemInstruction();

            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String response = analysisProvider.complete(systemInstruction, prompt);

            WritingStyleAnalysis analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Full profile writing style analysis completed in {}ms", processingTime.toMillis());

            return AnalysisResult.success(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                AnalysisGranularity.FULL_PROFILE,
                provider,
                analysis,
                LocalDateTime.now(),
                processingTime,
                analysisProvider.getLastCallMetadata()
            );

        } catch (Exception e) {
            log.error("Full profile writing style analysis failed: {}", e.getMessage(), e);
            Duration processingTime = Duration.between(start, Instant.now());
            return AnalysisResult.failure(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                AnalysisGranularity.FULL_PROFILE,
                provider,
                e.getMessage(),
                LocalDateTime.now(),
                processingTime
            );
        }
    }

    private WritingStyleAnalysis parseResponse(String response) throws Exception {
        // Extract JSON from potential markdown code blocks
        String json = extractJson(response);

        JsonNode root = objectMapper.readTree(json);

        return WritingStyleAnalysis.builder()
            .tone(root.path("tone").asText())
            .toneDescription(root.path("toneDescription").asText())
            .narrativeStructure(root.path("narrativeStructure").asText())
            .commonHooks(parseStringList(root, "commonHooks"))
            .emojiUsage(root.path("emojiUsage").asText())
            .vocabulary(root.path("vocabulary").asText())
            .frequentPhrases(parseStringList(root, "frequentPhrases"))
            .punctuationStyle(root.path("punctuationStyle").asText())
            .paragraphStructure(root.path("paragraphStructure").asText())
            .averagePostLength(root.path("averagePostLength").asInt(0))
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
        // Remove markdown code blocks if present
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
