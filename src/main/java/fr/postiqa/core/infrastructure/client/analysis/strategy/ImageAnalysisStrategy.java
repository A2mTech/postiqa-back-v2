package fr.postiqa.core.infrastructure.client.analysis.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.AnalysisResult;
import fr.postiqa.core.domain.model.analysis.ImageAnalysisResult;
import fr.postiqa.core.infrastructure.client.analysis.prompt.ImageAnalysisPromptBuilder;
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
 * Strategy for analyzing images and visual content
 */
@Component
public class ImageAnalysisStrategy implements AnalysisStrategy<ImageAnalysisResult> {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisStrategy.class);

    private final ImageAnalysisPromptBuilder promptBuilder;
    private final AnalysisProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public ImageAnalysisStrategy(
        ImageAnalysisPromptBuilder promptBuilder,
        AnalysisProviderRegistry providerRegistry,
        ObjectMapper objectMapper
    ) {
        this.promptBuilder = promptBuilder;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.IMAGE_ANALYSIS;
    }

    @Override
    public AnalysisResult<ImageAnalysisResult> analyzeSinglePost(SocialPost post, AIProvider provider) {
        log.debug("Analyzing image for single post from platform: {}", post.platform());
        Instant start = Instant.now();

        try {
            if (post.mediaUrls() == null || post.mediaUrls().isEmpty()) {
                throw new IllegalArgumentException("Post has no media to analyze");
            }

            String prompt = promptBuilder.buildForSinglePost(post);
            String systemInstruction = promptBuilder.getSystemInstruction();

            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String imageUrl = post.mediaUrls().get(0);
            String response = analysisProvider.analyzeImage(imageUrl, prompt, systemInstruction);

            ImageAnalysisResult analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Image analysis completed in {}ms", processingTime.toMillis());

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
            log.error("Image analysis failed: {}", e.getMessage(), e);
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
    public AnalysisResult<ImageAnalysisResult> analyzeBatchPosts(List<SocialPost> posts, AIProvider provider) {
        log.debug("Analyzing images for {} posts in batch", posts.size());
        Instant start = Instant.now();

        try {
            List<String> imageUrls = posts.stream()
                .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
                .flatMap(post -> post.mediaUrls().stream())
                .collect(Collectors.toList());

            if (imageUrls.isEmpty()) {
                throw new IllegalArgumentException("No images found in posts");
            }

            String prompt = promptBuilder.buildForBatchPosts(posts);
            String systemInstruction = promptBuilder.getSystemInstruction();

            AnalysisProvider analysisProvider = providerRegistry.getProvider(provider);
            String response = analysisProvider.analyzeImages(imageUrls, prompt, systemInstruction);

            ImageAnalysisResult analysis = parseResponse(response);

            Duration processingTime = Duration.between(start, Instant.now());
            log.debug("Batch image analysis completed in {}ms", processingTime.toMillis());

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
            log.error("Batch image analysis failed: {}", e.getMessage(), e);
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
    public AnalysisResult<ImageAnalysisResult> analyzeFullProfile(List<SocialPost> posts, AIProvider provider) {
        log.debug("Analyzing full image profile for {} posts", posts.size());
        return analyzeBatchPosts(posts, provider); // Same logic for full profile
    }

    private ImageAnalysisResult parseResponse(String response) throws Exception {
        String json = extractJson(response);
        JsonNode root = objectMapper.readTree(json);

        JsonNode brandingNode = root.path("brandingElements");
        ImageAnalysisResult.BrandingElements branding = new ImageAnalysisResult.BrandingElements(
            brandingNode.path("hasLogo").asBoolean(false),
            parseStringList(brandingNode, "brandColors"),
            brandingNode.path("brandStyle").asText(),
            brandingNode.path("consistency").asText()
        );

        return ImageAnalysisResult.builder()
            .visualDescription(root.path("visualDescription").asText())
            .detectedObjects(parseStringList(root, "detectedObjects"))
            .colors(parseStringList(root, "colors"))
            .composition(root.path("composition").asText())
            .style(root.path("style").asText())
            .brandingElements(branding)
            .emotionalImpact(root.path("emotionalImpact").asText())
            .targetContext(root.path("targetContext").asText())
            .suggestedText(parseStringList(root, "suggestedText"))
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
