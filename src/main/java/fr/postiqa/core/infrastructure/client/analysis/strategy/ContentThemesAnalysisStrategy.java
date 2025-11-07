package fr.postiqa.core.infrastructure.client.analysis.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.AnalysisResult;
import fr.postiqa.core.domain.model.analysis.ContentThemesAnalysis;
import fr.postiqa.core.infrastructure.client.analysis.prompt.ContentThemesPromptBuilder;
import fr.postiqa.core.infrastructure.client.analysis.provider.AnalysisProvider;
import fr.postiqa.core.infrastructure.client.analysis.registry.AnalysisProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Strategy for analyzing content themes and topics
 */
@Component
public class ContentThemesAnalysisStrategy implements AnalysisStrategy<ContentThemesAnalysis> {

    private static final Logger log = LoggerFactory.getLogger(ContentThemesAnalysisStrategy.class);

    private final ContentThemesPromptBuilder promptBuilder;
    private final AnalysisProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public ContentThemesAnalysisStrategy(
        ContentThemesPromptBuilder promptBuilder,
        AnalysisProviderRegistry providerRegistry,
        ObjectMapper objectMapper
    ) {
        this.promptBuilder = promptBuilder;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.CONTENT_THEMES;
    }

    @Override
    public AnalysisResult<ContentThemesAnalysis> analyzeSinglePost(SocialPost post, AIProvider provider) {
        log.debug("Analyzing content themes for single post from platform: {}", post.platform());
        return executeAnalysis(
            () -> promptBuilder.buildForSinglePost(post),
            AnalysisGranularity.SINGLE_POST,
            provider
        );
    }

    @Override
    public AnalysisResult<ContentThemesAnalysis> analyzeBatchPosts(List<SocialPost> posts, AIProvider provider) {
        log.debug("Analyzing content themes for {} posts in batch", posts.size());
        return executeAnalysis(
            () -> promptBuilder.buildForBatchPosts(posts),
            AnalysisGranularity.BATCH_POSTS,
            provider
        );
    }

    @Override
    public AnalysisResult<ContentThemesAnalysis> analyzeFullProfile(List<SocialPost> posts, AIProvider provider) {
        log.debug("Analyzing full content themes profile for {} posts", posts.size());
        return executeAnalysis(
            () -> promptBuilder.buildForFullProfile(posts),
            AnalysisGranularity.FULL_PROFILE,
            provider
        );
    }

    private AnalysisResult<ContentThemesAnalysis> executeAnalysis(
        PromptSupplier promptSupplier,
        AnalysisGranularity granularity,
        AIProvider provider
    ) {
        Instant start = Instant.now();

        try {
            String prompt = promptSupplier.get();
            String systemInstruction = promptBuilder.getSystemInstruction();

            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String response = analysisProvider.complete(systemInstruction, prompt);

            ContentThemesAnalysis analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Content themes analysis ({}) completed in {}ms", granularity, processingTime.toMillis());

            return AnalysisResult.success(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                granularity,
                provider,
                analysis,
                LocalDateTime.now(),
                processingTime,
                analysisProvider.getLastCallMetadata()
            );

        } catch (Exception e) {
            log.error("Content themes analysis failed: {}", e.getMessage(), e);
            Duration processingTime = Duration.between(start, Instant.now());
            return AnalysisResult.failure(
                UUID.randomUUID().toString(),
                getAnalysisType(),
                granularity,
                provider,
                e.getMessage(),
                LocalDateTime.now(),
                processingTime
            );
        }
    }

    private ContentThemesAnalysis parseResponse(String response) throws Exception {
        String json = extractJson(response);
        JsonNode root = objectMapper.readTree(json);

        return ContentThemesAnalysis.builder()
            .mainThemes(parseStringList(root, "mainThemes"))
            .secondaryThemes(parseStringList(root, "secondaryThemes"))
            .overallSentiment(root.path("overallSentiment").asText())
            .themeSentiments(parseStringMap(root, "themeSentiments"))
            .keyMessages(parseStringList(root, "keyMessages"))
            .callsToAction(parseStringList(root, "callsToAction"))
            .contentIntent(root.path("contentIntent").asText())
            .targetAudience(root.path("targetAudience").asText())
            .expertiseAreas(parseStringList(root, "expertiseAreas"))
            .themeFrequency(parseIntegerMap(root, "themeFrequency"))
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

    private Map<String, String> parseStringMap(JsonNode root, String fieldName) {
        Map<String, String> result = new HashMap<>();
        JsonNode mapNode = root.path(fieldName);
        if (mapNode.isObject()) {
            mapNode.fields().forEachRemaining(entry ->
                result.put(entry.getKey(), entry.getValue().asText()));
        }
        return result;
    }

    private Map<String, Integer> parseIntegerMap(JsonNode root, String fieldName) {
        Map<String, Integer> result = new HashMap<>();
        JsonNode mapNode = root.path(fieldName);
        if (mapNode.isObject()) {
            mapNode.fields().forEachRemaining(entry ->
                result.put(entry.getKey(), entry.getValue().asInt(0)));
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

    @FunctionalInterface
    private interface PromptSupplier {
        String get() throws Exception;
    }
}
