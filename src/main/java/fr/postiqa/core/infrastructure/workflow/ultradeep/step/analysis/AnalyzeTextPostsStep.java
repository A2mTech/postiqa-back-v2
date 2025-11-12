package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.PostType;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import fr.postiqa.database.entity.PostAnalysisEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Step 2C.1: Analyze TEXT posts (posts without media).
 * <p>
 * Analyzes: hook, body structure, CTA, writing style, formatting, tone, engagement drivers.
 */
@Slf4j
@Component
public class AnalyzeTextPostsStep implements WorkflowStep<Void, Map<String, Object>> {

    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    private static final List<SocialPlatform> ALL_PLATFORMS = List.of(
        SocialPlatform.LINKEDIN,
        SocialPlatform.TWITTER,
        SocialPlatform.INSTAGRAM,
        SocialPlatform.YOUTUBE,
        SocialPlatform.TIKTOK
    );

    public AnalyzeTextPostsStep(
        OpenAIClient openAIClient,
        JpaAnalysisResultsAdapter analysisAdapter,
        ObjectMapper objectMapper
    ) {
        this.openAIClient = openAIClient;
        this.analysisAdapter = analysisAdapter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "analyze-text-posts";
    }

    @Override
    public String getStepName() {
        return "Analyze Text Posts";
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
            List<SocialPost> textPosts = allPosts.stream()
                .filter(post -> post.postType() == PostType.TEXT)
                .limit(20) // Analyze up to 20 text posts per platform
                .collect(Collectors.toList());

            if (textPosts.isEmpty()) {
                log.debug("No text posts found for {}", platform);
                continue;
            }

            log.info("Analyzing {} text posts for {}", textPosts.size(), platform);

            List<Map<String, Object>> analyses = analyzePostsBatch(textPosts, platform, context);
            allResults.put(platform.name(), analyses);
            totalAnalyzed += analyses.size();

            log.info("Completed analysis of {} text posts for {}", analyses.size(), platform);
        }

        log.info("Total text posts analyzed across all platforms: {}", totalAnalyzed);
        return Map.of(
            "total_analyzed", totalAnalyzed,
            "results_by_platform", allResults
        );
    }

    private List<Map<String, Object>> analyzePostsBatch(
        List<SocialPost> posts,
        SocialPlatform platform,
        WorkflowContext context
    ) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        // Analyze in batches of 10 for better context management
        int batchSize = 10;
        for (int i = 0; i < posts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, posts.size());
            List<SocialPost> batch = posts.subList(i, end);

            String prompt = buildTextPostsPrompt(batch, platform);
            String systemInstruction = """
                You are an expert content analyst. Analyze social media text posts with focus on:
                - Hook: Opening line effectiveness, attention-grabbing techniques
                - Body Structure: Flow, paragraph structure, storytelling
                - Call-to-Action: Presence, clarity, effectiveness
                - Writing Style: Tone, voice, vocabulary, personality
                - Formatting: Line breaks, emojis, punctuation patterns
                - Engagement Drivers: What makes this content engaging?

                Return a JSON array with one analysis object per post, in the same order.
                """;

            String response = openAIClient.complete(systemInstruction, prompt);
            List<Map<String, Object>> batchResults = parseJsonArrayResponse(response);

            results.addAll(batchResults);
        }

        return results;
    }

    private String buildTextPostsPrompt(List<SocialPost> posts, SocialPlatform platform) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Analyze the following %d text posts from %s:\n\n",
            posts.size(), platform.getDisplayName()));

        for (int i = 0; i < posts.size(); i++) {
            SocialPost post = posts.get(i);
            prompt.append(String.format("--- POST %d ---\n", i + 1));
            prompt.append(post.content()).append("\n");
            prompt.append(String.format("Engagement: %d likes, %d comments\n\n",
                post.engagementMetrics().getOrDefault("likes", 0),
                post.engagementMetrics().getOrDefault("comments", 0)));
        }

        prompt.append("""

            For each post, provide:
            {
              "post_index": <number>,
              "hook_analysis": {
                "hook_text": "<first line>",
                "technique": "<technique used>",
                "effectiveness_score": <1-10>
              },
              "body_structure": {
                "paragraph_count": <number>,
                "structure_type": "<e.g., story-lesson, direct, question-answer>",
                "flow_quality": "<smooth/choppy/excellent>"
              },
              "call_to_action": {
                "has_cta": <true/false>,
                "cta_text": "<CTA if present>",
                "cta_type": "<e.g., question, command, invitation>"
              },
              "writing_style": {
                "tone": "<e.g., professional, casual, inspirational>",
                "voice": "<e.g., authoritative, friendly, vulnerable>",
                "personality_traits": ["<trait1>", "<trait2>"]
              },
              "formatting": {
                "emoji_usage": "<description>",
                "line_breaks": "<frequent/sparse/strategic>",
                "punctuation_style": "<description>"
              },
              "engagement_drivers": ["<driver1>", "<driver2>"],
              "replicability_score": <1-10>,
              "key_insights": ["<insight1>", "<insight2>"]
            }
            """);

        return prompt.toString();
    }

    private List<Map<String, Object>> parseJsonArrayResponse(String response) throws Exception {
        String jsonString = response.trim();
        if (jsonString.startsWith("```json")) {
            jsonString = jsonString.substring(7);
        } else if (jsonString.startsWith("```")) {
            jsonString = jsonString.substring(3);
        }
        if (jsonString.endsWith("```")) {
            jsonString = jsonString.substring(0, jsonString.length() - 3);
        }
        jsonString = jsonString.trim();

        return objectMapper.readValue(jsonString, List.class);
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("text_posts_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(30); // Long timeout for batch processing
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(5));
    }
}
