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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Step 2C.5: Analyze THREAD posts (Twitter threads mainly).
 * <p>
 * Analyzes thread structure, progression, and hook-to-conclusion flow.
 * Focuses on storytelling across multiple connected tweets, information architecture, and engagement tactics.
 */
@Slf4j
@Component
public class AnalyzeThreadPostsStep implements WorkflowStep<Void, Map<String, Object>> {

    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    private static final List<SocialPlatform> THREAD_PLATFORMS = List.of(
        SocialPlatform.TWITTER
    );

    public AnalyzeThreadPostsStep(
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
        return "analyze-thread-posts";
    }

    @Override
    public String getStepName() {
        return "Analyze Thread Posts";
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

        for (SocialPlatform platform : THREAD_PLATFORMS) {
            String postsKey = platform.name().toLowerCase() + "_posts";
            Optional<List<SocialPost>> postsOpt = getFromContext(context, postsKey, List.class);

            if (postsOpt.isEmpty()) {
                continue;
            }

            List<SocialPost> allPosts = postsOpt.get();
            List<SocialPost> threadPosts = allPosts.stream()
                .filter(post -> post.postType() == PostType.THREAD)
                .filter(post -> post.threadContent() != null && !post.threadContent().isEmpty())
                .limit(20) // Analyze up to 20 threads per platform
                .collect(Collectors.toList());

            if (threadPosts.isEmpty()) {
                log.debug("No thread posts found for {}", platform);
                continue;
            }

            log.info("Analyzing {} thread posts for {}", threadPosts.size(), platform);

            List<Map<String, Object>> analyses = analyzeThreadsBatch(threadPosts, platform, context);
            allResults.put(platform.name(), analyses);
            totalAnalyzed += analyses.size();

            log.info("Completed analysis of {} thread posts for {}", analyses.size(), platform);
        }

        log.info("Total thread posts analyzed across all platforms: {}", totalAnalyzed);
        return Map.of(
            "total_analyzed", totalAnalyzed,
            "results_by_platform", allResults
        );
    }

    private List<Map<String, Object>> analyzeThreadsBatch(
        List<SocialPost> threads,
        SocialPlatform platform,
        WorkflowContext context
    ) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        // Analyze in batches of 5 threads for better context management (threads are longer)
        int batchSize = 5;
        for (int i = 0; i < threads.size(); i += batchSize) {
            int end = Math.min(i + batchSize, threads.size());
            List<SocialPost> batch = threads.subList(i, end);

            String prompt = buildThreadsPrompt(batch, platform);
            String systemInstruction = """
                You are an expert Twitter thread analyst. Analyze threads with focus on:
                - Hook: First tweet effectiveness, attention-grabbing, curiosity gap
                - Thread Structure: Flow, progression, information architecture
                - Storytelling: Narrative arc, pacing, retention techniques
                - Engagement Tactics: Pattern breaks, cliffhangers, CTAs
                - Educational Value: Learning takeaways, actionability
                - Thread-to-Tweet Transition: How well each tweet flows to the next

                Return a JSON array with one analysis object per thread, in the same order.
                """;

            String response = openAIClient.complete(systemInstruction, prompt);
            List<Map<String, Object>> batchResults = parseJsonArrayResponse(response);

            results.addAll(batchResults);
        }

        return results;
    }

    private String buildThreadsPrompt(List<SocialPost> threads, SocialPlatform platform) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Analyze the following %d threads from %s:\n\n",
            threads.size(), platform.getDisplayName()));

        for (int i = 0; i < threads.size(); i++) {
            SocialPost thread = threads.get(i);
            List<String> tweets = thread.threadContent();

            prompt.append(String.format("--- THREAD %d (%d tweets) ---\n", i + 1, tweets.size()));

            for (int j = 0; j < tweets.size(); j++) {
                prompt.append(String.format("Tweet %d/%d:\n", j + 1, tweets.size()));
                prompt.append(tweets.get(j)).append("\n\n");
            }

            prompt.append(String.format("Engagement: %d likes, %d retweets, %d replies\n\n",
                thread.engagementMetrics().getOrDefault("likes", 0),
                thread.engagementMetrics().getOrDefault("retweets", 0),
                thread.engagementMetrics().getOrDefault("replies", 0)));
        }

        prompt.append("""

            For each thread, provide:
            {
              "thread_index": <number>,
              "tweet_count": <number>,
              "hook_analysis": {
                "first_tweet": "<first tweet text>",
                "hook_technique": "<technique used: question, stat, story, controversial, etc.>",
                "curiosity_gap": "<how it creates curiosity>",
                "hook_effectiveness_score": <1-10>
              },
              "thread_structure": {
                "structure_type": "<e.g., story-lesson, problem-solution, list, framework>",
                "flow_quality": "<smooth/choppy/excellent>",
                "information_architecture": "<how information is organized>",
                "pacing": "<fast/medium/slow>"
              },
              "tweet_to_tweet_transitions": {
                "transition_quality_score": <1-10>,
                "transition_techniques": ["<technique1>", "<technique2>"],
                "cliffhanger_usage": "<description>"
              },
              "storytelling": {
                "narrative_arc": "<description of story progression>",
                "emotional_journey": "<how emotions evolve through thread>",
                "retention_techniques": ["<technique1>", "<technique2>"]
              },
              "engagement_tactics": {
                "pattern_breaks": ["<pattern break 1>", "<pattern break 2>"],
                "visual_elements": "<use of emojis, spacing, formatting>",
                "cta_placement": "<where and how CTAs are used>",
                "engagement_drivers": ["<driver1>", "<driver2>"]
              },
              "educational_value": {
                "key_takeaways": ["<takeaway1>", "<takeaway2>"],
                "actionability": "<1-10>",
                "depth_score": "<1-10>"
              },
              "conclusion_effectiveness": {
                "last_tweet": "<last tweet text>",
                "conclusion_type": "<summary, CTA, question, etc.>",
                "effectiveness_score": <1-10>
              },
              "writing_style": {
                "tone": "<e.g., educational, conversational, authoritative>",
                "voice": "<e.g., personal, professional, casual>",
                "personality_traits": ["<trait1>", "<trait2>"]
              },
              "performance_analysis": {
                "engagement_score": <1-10>,
                "virality_potential": "<low/medium/high>",
                "performance_drivers": ["<driver1>", "<driver2>"]
              },
              "replicability": {
                "replicability_score": <1-10>,
                "key_patterns_to_replicate": ["<pattern1>", "<pattern2>"],
                "complexity": "<simple/moderate/complex>"
              },
              "recommendations": {
                "hook_improvements": ["<improvement1>", "<improvement2>"],
                "structure_optimizations": ["<optimization1>", "<optimization2>"],
                "engagement_enhancements": ["<enhancement1>", "<enhancement2>"]
              }
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
        return Optional.of("thread_posts_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(35); // Threads require detailed analysis
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(5));
    }
}
