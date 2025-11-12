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
 * Step 2C.2: Analyze IMAGE posts (single image + text).
 * <p>
 * Uses GPT-4 Vision + multimodal analysis to understand:
 * - Text-image alignment and coherence
 * - Visual storytelling
 * - Brand consistency
 * - Engagement optimization
 */
@Slf4j
@Component
public class AnalyzeImagePostsStep implements WorkflowStep<Void, Map<String, Object>> {

    private final GPT4VisionClient visionClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    private static final List<SocialPlatform> ALL_PLATFORMS = List.of(
        SocialPlatform.LINKEDIN,
        SocialPlatform.TWITTER,
        SocialPlatform.INSTAGRAM,
        SocialPlatform.YOUTUBE,
        SocialPlatform.TIKTOK
    );

    public AnalyzeImagePostsStep(
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
        return "analyze-image-posts";
    }

    @Override
    public String getStepName() {
        return "Analyze Image Posts";
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

        for (SocialPlatform platform : ALL_PLATFORMS) {
            String postsKey = platform.name().toLowerCase() + "_posts";
            Optional<List<SocialPost>> postsOpt = getFromContext(context, postsKey, List.class);

            if (postsOpt.isEmpty()) {
                continue;
            }

            List<SocialPost> allPosts = postsOpt.get();
            List<SocialPost> imagePosts = allPosts.stream()
                .filter(post -> post.postType() == PostType.IMAGE)
                .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
                .limit(20) // Analyze up to 20 image posts per platform
                .collect(Collectors.toList());

            if (imagePosts.isEmpty()) {
                log.debug("No image posts found for {}", platform);
                continue;
            }

            log.info("Analyzing {} image posts for {}", imagePosts.size(), platform);

            List<Map<String, Object>> analyses = new ArrayList<>();
            for (SocialPost post : imagePosts) {
                Map<String, Object> analysis = analyzeImagePost(post, platform);
                analyses.add(analysis);
            }

            allResults.put(platform.name(), analyses);
            totalAnalyzed += analyses.size();

            log.info("Completed analysis of {} image posts for {}", analyses.size(), platform);
        }

        log.info("Total image posts analyzed across all platforms: {}", totalAnalyzed);
        return Map.of(
            "total_analyzed", totalAnalyzed,
            "results_by_platform", allResults
        );
    }

    private Map<String, Object> analyzeImagePost(SocialPost post, SocialPlatform platform) throws Exception {
        String imageUrl = post.mediaUrls().get(0); // Single image
        String textContent = post.content() != null ? post.content() : "";

        String prompt = String.format("""
            Analyze this %s image post combining both the image and text:

            Text Content:
            %s

            Engagement: %d likes, %d comments

            Provide a comprehensive multimodal analysis:

            1. **Image Analysis**:
               - Visual description and composition
               - Main subject and focal points
               - Colors, style, and mood
               - Technical quality
               - Brand elements present

            2. **Text-Image Alignment**:
               - How well does the text complement the image?
               - Alignment score (1-10)
               - Coherence and synergy

            3. **Content Analysis**:
               - Hook effectiveness in text
               - Message clarity
               - Story told through image + text combined

            4. **Visual Storytelling**:
               - Narrative flow
               - Emotional impact
               - Memorability

            5. **Engagement Optimization**:
               - What drives engagement in this post?
               - Scroll-stopping elements
               - Call-to-action effectiveness

            6. **Brand Consistency**:
               - Visual brand alignment
               - Tone consistency
               - Professional quality

            7. **Replicability**:
               - Score (1-10) for how easily this style can be replicated
               - Key patterns to replicate

            8. **Recommendations**:
               - Specific improvements for text
               - Visual enhancements
               - Engagement optimization tips

            Return a structured JSON response.
            """,
            platform.getDisplayName(),
            textContent,
            post.engagementMetrics().getOrDefault("likes", 0),
            post.engagementMetrics().getOrDefault("comments", 0)
        );

        Map<String, Object> result = visionClient.analyzeImageWithText(imageUrl, textContent, prompt);

        log.debug("Analyzed image post: {} chars, {} engagement",
            textContent.length(),
            post.engagementMetrics().getOrDefault("likes", 0));

        return result;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("image_posts_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(40); // Vision API calls take time
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(5));
    }
}
