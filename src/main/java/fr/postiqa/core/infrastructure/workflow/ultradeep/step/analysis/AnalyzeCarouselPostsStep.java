package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.PostType;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.GPT4VisionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Step 2C.3: Analyze CAROUSEL posts (multi-slide posts, mainly Instagram/LinkedIn).
 * <p>
 * Uses GPT-4 Vision to analyze each slide in sequence and builds narrative flow analysis across slides.
 * Evaluates visual storytelling, progression, and educational content delivery.
 */
@Slf4j
@Component
public class AnalyzeCarouselPostsStep implements WorkflowStep<Void, Map<String, Object>> {

    private final GPT4VisionClient visionClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    private static final List<SocialPlatform> CAROUSEL_PLATFORMS = List.of(
        SocialPlatform.INSTAGRAM,
        SocialPlatform.LINKEDIN
    );

    public AnalyzeCarouselPostsStep(
        GPT4VisionClient visionClient,
        JpaAnalysisResultsAdapter analysisAdapter,
        ObjectMapper objectMapper
    ) {
        this.visionClient = visionClient;
        this.analysisAdapter = analysisAdapter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "analyze-carousel-posts";
    }

    @Override
    public String getStepName() {
        return "Analyze Carousel Posts";
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getFromContext(WorkflowContext context, String key, Class<?> type) {
        return (Optional<T>) (Optional<?>) context.get(key, type);
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);

        Map<String, Object> allResults = new HashMap<>();
        int totalAnalyzed = 0;

        for (SocialPlatform platform : CAROUSEL_PLATFORMS) {
            String postsKey = platform.name().toLowerCase() + "_posts";
            Optional<List<SocialPost>> postsOpt = getFromContext(context, postsKey, List.class);

            if (postsOpt.isEmpty()) {
                continue;
            }

            List<SocialPost> allPosts = postsOpt.get();
            List<SocialPost> carouselPosts = allPosts.stream()
                .filter(post -> post.postType() == PostType.CAROUSEL)
                .filter(post -> post.mediaUrls() != null && post.mediaUrls().size() > 1)
                .limit(15) // Analyze up to 15 carousel posts per platform
                .collect(Collectors.toList());

            if (carouselPosts.isEmpty()) {
                log.debug("No carousel posts found for {}", platform);
                continue;
            }

            log.info("Analyzing {} carousel posts for {}", carouselPosts.size(), platform);

            List<Map<String, Object>> analyses = new ArrayList<>();
            for (SocialPost post : carouselPosts) {
                Map<String, Object> analysis = analyzeCarouselPost(post, platform);
                analyses.add(analysis);
            }

            allResults.put(platform.name(), analyses);
            totalAnalyzed += analyses.size();

            log.info("Completed analysis of {} carousel posts for {}", analyses.size(), platform);
        }

        log.info("Total carousel posts analyzed across all platforms: {}", totalAnalyzed);
        return Map.of(
            "total_analyzed", totalAnalyzed,
            "results_by_platform", allResults
        );
    }

    private Map<String, Object> analyzeCarouselPost(SocialPost post, SocialPlatform platform) throws Exception {
        List<String> slideUrls = post.mediaUrls();
        String textContent = post.content() != null ? post.content() : "";
        int slideCount = slideUrls.size();

        log.debug("Analyzing carousel post with {} slides from {}", slideCount, platform);

        // Step 1: Analyze each slide individually
        List<Map<String, Object>> slideAnalyses = new ArrayList<>();
        for (int i = 0; i < slideUrls.size(); i++) {
            String slideUrl = slideUrls.get(i);
            int slideNumber = i + 1;

            String slidePrompt = String.format("""
                Analyze slide %d of %d from a %s carousel post:

                Context: This is part of a multi-slide carousel. Analyze this individual slide for:

                1. **Visual Content**:
                   - Main message or topic
                   - Design quality and composition
                   - Text readability and hierarchy
                   - Visual elements (images, icons, graphics)

                2. **Information Delivery**:
                   - Clarity of message
                   - Information density
                   - Hook effectiveness (if first slide)
                   - CTA effectiveness (if last slide)

                3. **Design Consistency**:
                   - Brand elements present
                   - Color scheme and typography
                   - Visual style

                Return a structured JSON response with these insights.
                """,
                slideNumber,
                slideCount,
                platform.getDisplayName()
            );

            Map<String, Object> slideAnalysis = visionClient.analyzeImage(slideUrl, slidePrompt);
            slideAnalysis.put("slide_number", slideNumber);
            slideAnalyses.add(slideAnalysis);

            log.debug("Analyzed slide {}/{}", slideNumber, slideCount);
        }

        // Step 2: Synthesize all slides into narrative flow analysis
        Map<String, Object> narrativeAnalysis = synthesizeCarouselNarrative(
            post,
            platform,
            slideAnalyses,
            textContent
        );

        // Combine individual slide analyses with narrative analysis
        Map<String, Object> completeAnalysis = new HashMap<>();
        completeAnalysis.put("post_id", post.postId());
        completeAnalysis.put("platform", platform.name());
        completeAnalysis.put("slide_count", slideCount);
        completeAnalysis.put("caption", textContent);
        completeAnalysis.put("engagement_metrics", post.engagementMetrics());
        completeAnalysis.put("slide_analyses", slideAnalyses);
        completeAnalysis.put("narrative_analysis", narrativeAnalysis);

        return completeAnalysis;
    }

    private Map<String, Object> synthesizeCarouselNarrative(
        SocialPost post,
        SocialPlatform platform,
        List<Map<String, Object>> slideAnalyses,
        String caption
    ) throws Exception {
        // Build comprehensive prompt with all slide analyses
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("""
            Synthesize this %s carousel post with %d slides into a complete narrative flow analysis.

            CAPTION:
            %s

            ENGAGEMENT:
            %d likes, %d comments, %d shares

            INDIVIDUAL SLIDE ANALYSES:
            """,
            platform.getDisplayName(),
            slideAnalyses.size(),
            caption,
            post.engagementMetrics().getOrDefault("likes", 0),
            post.engagementMetrics().getOrDefault("comments", 0),
            post.engagementMetrics().getOrDefault("shares", 0)
        ));

        for (int i = 0; i < slideAnalyses.size(); i++) {
            prompt.append(String.format("\n--- SLIDE %d ANALYSIS ---\n", i + 1));
            prompt.append(objectMapper.writeValueAsString(slideAnalyses.get(i))).append("\n");
        }

        prompt.append("""

            Provide a comprehensive carousel narrative analysis:

            1. **Narrative Flow**:
               - How well do the slides flow from one to another?
               - Story progression quality
               - Hook-to-conclusion effectiveness
               - Flow score (1-10)

            2. **Content Strategy**:
               - Type of carousel (educational, storytelling, promotional, etc.)
               - Information architecture
               - Value delivery per slide
               - Pacing and information density

            3. **Visual Storytelling**:
               - Visual consistency across slides
               - Design quality and professionalism
               - Brand identity strength
               - Visual hierarchy effectiveness

            4. **Engagement Optimization**:
               - Hook strength (slide 1)
               - Retention techniques
               - CTA effectiveness (last slide)
               - Swipe-through motivation

            5. **Educational Value**:
               - Learning takeaways
               - Actionability of content
               - Depth vs. accessibility balance

            6. **Caption-Carousel Alignment**:
               - How well does the caption complement the carousel?
               - Alignment score (1-10)

            7. **Replicability**:
               - Replicability score (1-10)
               - Key patterns to replicate
               - Production complexity assessment
               - Template-ability

            8. **Performance Drivers**:
               - What makes this carousel successful/unsuccessful?
               - Specific engagement drivers

            9. **Recommendations**:
               - Content improvements
               - Design enhancements
               - Engagement optimization tips

            Return a detailed JSON response.
            """);

        String systemInstruction = """
            You are an expert carousel content analyst specializing in Instagram and LinkedIn.
            You understand visual storytelling, educational content design, and multi-slide narrative flow.
            Provide specific, actionable insights based on the slide-by-slide analysis.
            """;

        String response = visionClient.completeTextOnly(systemInstruction, prompt.toString());

        // Parse JSON response
        String jsonString = response.trim();
        if (jsonString.startsWith("```json")) {
            jsonString = jsonString.substring(7);
        } else if (jsonString.startsWith("```")) {
            jsonString = jsonString.substring(3);
        }
        if (jsonString.endsWith("```")) {
            jsonString = jsonString.substring(0, jsonString.length() - 3);
        }

        return objectMapper.readValue(jsonString.trim(), Map.class);
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("carousel_posts_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(50); // Vision API calls for multiple slides take time
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(5));
    }
}
