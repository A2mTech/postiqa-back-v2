package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.PostType;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.port.TranscriptionPort;
import fr.postiqa.core.domain.port.VisionAnalysisPort;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import fr.postiqa.core.infrastructure.client.WhisperClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Step 2C.4: Analyze VIDEO posts (most complex analysis).
 * <p>
 * Multi-step process:
 * 1. Transcribe audio with Whisper
 * 2. Extract keyframes or analyze thumbnail
 * 3. Analyze visual frames with GPT-4 Vision
 * 4. Synthesize transcription + visuals with LLM for complete analysis
 * <p>
 * Analyzes: script structure, visual storytelling, pacing, hooks, engagement patterns.
 */
@Slf4j
@Component
public class AnalyzeVideoPostsStep implements WorkflowStep<Void, Map<String, Object>> {

    private final WhisperClient whisperClient;
    private final VisionAnalysisPort visionAnalysisPort;
    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    private static final List<SocialPlatform> VIDEO_PLATFORMS = List.of(
        SocialPlatform.YOUTUBE,
        SocialPlatform.TIKTOK,
        SocialPlatform.INSTAGRAM // Reels
    );

    public AnalyzeVideoPostsStep(
        WhisperClient whisperClient,
        VisionAnalysisPort visionAnalysisPort,
        OpenAIClient openAIClient,
        JpaAnalysisResultsAdapter analysisAdapter,
        ObjectMapper objectMapper
    ) {
        this.whisperClient = whisperClient;
        this.visionAnalysisPort = visionAnalysisPort;
        this.openAIClient = openAIClient;
        this.analysisAdapter = analysisAdapter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "analyze-video-posts";
    }

    @Override
    public String getStepName() {
        return "Analyze Video Posts";
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

        for (SocialPlatform platform : VIDEO_PLATFORMS) {
            String postsKey = platform.name().toLowerCase() + "_posts";
            Optional<List<SocialPost>> postsOpt = getFromContext(context, postsKey, List.class);

            if (postsOpt.isEmpty()) {
                continue;
            }

            List<SocialPost> allPosts = postsOpt.get();
            List<SocialPost> videoPosts = allPosts.stream()
                .filter(post -> post.postType() == PostType.VIDEO)
                .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
                .limit(10) // Limit to 10 videos per platform (expensive analysis)
                .collect(Collectors.toList());

            if (videoPosts.isEmpty()) {
                log.debug("No video posts found for {}", platform);
                continue;
            }

            log.info("Analyzing {} video posts for {} (this may take a while...)",
                videoPosts.size(), platform);

            List<Map<String, Object>> analyses = new ArrayList<>();
            for (SocialPost post : videoPosts) {
                try {
                    Map<String, Object> analysis = analyzeVideoPost(post, platform);
                    analyses.add(analysis);
                } catch (Exception e) {
                    log.error("Failed to analyze video post {}: {}", post.postId(), e.getMessage());
                    // Continue with other videos
                }
            }

            allResults.put(platform.name(), analyses);
            totalAnalyzed += analyses.size();

            log.info("Completed analysis of {} video posts for {}", analyses.size(), platform);
        }

        log.info("Total video posts analyzed across all platforms: {}", totalAnalyzed);
        return Map.of(
            "total_analyzed", totalAnalyzed,
            "results_by_platform", allResults
        );
    }

    private Map<String, Object> analyzeVideoPost(SocialPost post, SocialPlatform platform) throws Exception {
        String videoUrl = post.mediaUrls().get(0);
        log.info("Analyzing video post: {} from {}", post.postId(), platform);

        // Step 1: Transcribe audio with Whisper
        log.debug("Step 1/3: Transcribing video audio...");
        TranscriptionPort.TranscriptionResult transcription = whisperClient.transcribe(videoUrl);

        // Step 2: Analyze thumbnail/keyframe with Vision
        log.debug("Step 2/3: Analyzing video frames...");
        String thumbnailUrl = extractThumbnailUrl(post);
        Map<String, Object> visualAnalysis = null;
        if (thumbnailUrl != null) {
            String visualPrompt = """
                Analyze this video thumbnail/frame:
                - Visual composition and framing
                - Subject and focal points
                - Text overlays or graphics
                - Brand elements
                - Visual style and quality
                - Thumbnail effectiveness (clickability)

                Return JSON with these insights.
                """;
            visualAnalysis = visionAnalysisPort.analyzeImage(thumbnailUrl, visualPrompt);
        }

        // Step 3: Synthesize transcription + visuals with LLM
        log.debug("Step 3/3: Synthesizing complete video analysis...");
        Map<String, Object> completeAnalysis = synthesizeVideoAnalysis(
            post,
            platform,
            transcription,
            visualAnalysis
        );

        log.info("Video analysis completed for post {}", post.postId());
        return completeAnalysis;
    }

    private String extractThumbnailUrl(SocialPost post) {
        // Try to extract thumbnail URL from post metadata
        if (post.metadata() != null && post.metadata().containsKey("thumbnail_url")) {
            return (String) post.metadata().get("thumbnail_url");
        }
        // Could also generate thumbnail from video URL with external service
        return null;
    }

    private Map<String, Object> synthesizeVideoAnalysis(
        SocialPost post,
        SocialPlatform platform,
        TranscriptionPort.TranscriptionResult transcription,
        Map<String, Object> visualAnalysis
    ) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Analyze this %s video post comprehensively:\n\n", platform.getDisplayName()));

        prompt.append("VIDEO METADATA:\n");
        prompt.append(String.format("- Duration: %.1f seconds\n", transcription.duration()));
        prompt.append(String.format("- Language: %s\n", transcription.detectedLanguage()));
        prompt.append(String.format("- Engagement: %d views, %d likes, %d comments\n\n",
            post.engagementMetrics().getOrDefault("views", 0),
            post.engagementMetrics().getOrDefault("likes", 0),
            post.engagementMetrics().getOrDefault("comments", 0)));

        prompt.append("TRANSCRIPTION:\n");
        prompt.append(transcription.fullText()).append("\n\n");

        if (visualAnalysis != null) {
            prompt.append("VISUAL ANALYSIS:\n");
            prompt.append(objectMapper.writeValueAsString(visualAnalysis)).append("\n\n");
        }

        if (post.content() != null && !post.content().isBlank()) {
            prompt.append("CAPTION/DESCRIPTION:\n");
            prompt.append(post.content()).append("\n\n");
        }

        prompt.append("""
            Provide a comprehensive video content analysis:

            1. **Script Analysis**:
               - Hook (first 3 seconds)
               - Script structure and flow
               - Key messages and storytelling
               - Pacing and timing

            2. **Visual Storytelling**:
               - Visual narrative techniques
               - Camera work and framing
               - Text overlays and graphics effectiveness
               - Visual hooks

            3. **Engagement Patterns**:
               - What makes this video engaging?
               - Retention techniques
               - Pattern breaks and surprises
               - Call-to-action effectiveness

            4. **Content Style**:
               - Tone and personality
               - Production quality
               - Authenticity vs. polish
               - Platform optimization

            5. **Replicability Analysis**:
               - Replicability score (1-10)
               - Key elements to replicate
               - Production complexity

            6. **Performance Drivers**:
               - Why this video performed well/poorly
               - Engagement optimization opportunities

            7. **Recommendations**:
               - Script improvements
               - Visual enhancements
               - Engagement tactics

            Return a detailed JSON response.
            """);

        String systemInstruction = """
            You are an expert video content analyst with deep knowledge of social media algorithms,
            audience engagement, and content strategy. Provide actionable, specific insights.
            """;

        String response = openAIClient.complete(systemInstruction, prompt.toString());

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

        Map<String, Object> analysis = objectMapper.readValue(jsonString.trim(), Map.class);

        // Add transcription data to result
        analysis.put("transcription", Map.of(
            "full_text", transcription.fullText(),
            "duration", transcription.duration(),
            "language", transcription.detectedLanguage(),
            "segment_count", transcription.segments().size()
        ));

        return analysis;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("video_posts_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofHours(1); // Videos take a long time (transcription + vision + synthesis)
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(1, Duration.ofSeconds(10)); // Only 1 retry, videos are expensive
    }
}
