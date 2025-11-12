package fr.postiqa.core.infrastructure.workflow.ultradeep.step.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import fr.postiqa.database.entity.PlatformSummaryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 3A: Generate platform summaries by aggregating profile + posts analyses per platform.
 * <p>
 * For each social platform:
 * - Aggregates profile analysis
 * - Aggregates all post type analyses (text, image, video, carousel, thread)
 * - Uses OpenAI to synthesize into comprehensive platform summary
 * - Saves to database
 * <p>
 * Output: Map of SocialPlatform -> PlatformSummaryResult
 */
@Slf4j
@Component
public class GeneratePlatformSummaryStep implements WorkflowStep<Void, Map<String, Map<String, Object>>> {

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

    public GeneratePlatformSummaryStep(
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
        return "generate-platform-summary";
    }

    @Override
    public String getStepName() {
        return "Generate Platform Summaries";
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getFromContext(WorkflowContext context, String key, Class<?> type) {
        return (Optional<T>) (Optional<?>) context.get(key, type);
    }

    @Override
    public Map<String, Map<String, Object>> execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);

        Map<String, Map<String, Object>> platformSummaries = new HashMap<>();

        for (SocialPlatform platform : ALL_PLATFORMS) {
            if (!hasPlatformData(platform, context)) {
                log.debug("No data found for {}, skipping platform summary", platform);
                continue;
            }

            log.info("Generating platform summary for {}", platform);

            Map<String, Object> summary = generatePlatformSummary(platform, context, userProfileAnalysisId);
            platformSummaries.put(platform.name(), summary);

            log.info("Completed platform summary for {}", platform);
        }

        log.info("Generated {} platform summaries", platformSummaries.size());
        return platformSummaries;
    }

    private boolean hasPlatformData(SocialPlatform platform, WorkflowContext context) {
        String postsKey = platform.name().toLowerCase() + "_posts";
        String profileKey = platform.name().toLowerCase() + "_profile";

        return getFromContext(context, postsKey, List.class).isPresent() ||
               getFromContext(context, profileKey, Map.class).isPresent();
    }

    private Map<String, Object> generatePlatformSummary(
        SocialPlatform platform,
        WorkflowContext context,
        UUID userProfileAnalysisId
    ) throws Exception {
        // Gather all platform data
        String platformKey = platform.name().toLowerCase();

        Optional<Map<String, Object>> profileOpt = getFromContext(context, platformKey + "_profile", Map.class);
        Optional<Map<String, Object>> textAnalysisOpt = getFromContext(context, "text_posts_analyses", Map.class);
        Optional<Map<String, Object>> imageAnalysisOpt = getFromContext(context, "image_posts_analyses", Map.class);
        Optional<Map<String, Object>> videoAnalysisOpt = getFromContext(context, "video_posts_analyses", Map.class);
        Optional<Map<String, Object>> carouselAnalysisOpt = getFromContext(context, "carousel_posts_analyses", Map.class);
        Optional<Map<String, Object>> threadAnalysisOpt = getFromContext(context, "thread_posts_analyses", Map.class);

        // Build comprehensive prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("""
            Synthesize all available data for %s into a comprehensive platform summary.

            This summary will be used to understand the user's complete presence and content strategy on this platform.

            """, platform.getDisplayName()));

        // Add profile analysis if available
        if (profileOpt.isPresent()) {
            Map<String, Object> profileData = profileOpt.get();
            prompt.append("=== PROFILE ANALYSIS ===\n");
            prompt.append(objectMapper.writeValueAsString(profileData)).append("\n\n");
        }

        // Add post analyses by type
        int totalPostsAnalyzed = 0;
        Map<String, Object> allPostAnalyses = new HashMap<>();

        if (textAnalysisOpt.isPresent()) {
            Map<String, Object> textData = textAnalysisOpt.get();
            Object platformData = ((Map<String, Object>) textData.get("results_by_platform")).get(platform.name());
            if (platformData != null) {
                allPostAnalyses.put("text_posts", platformData);
                totalPostsAnalyzed += ((List<?>) platformData).size();
            }
        }

        if (imageAnalysisOpt.isPresent()) {
            Map<String, Object> imageData = imageAnalysisOpt.get();
            Object platformData = ((Map<String, Object>) imageData.get("results_by_platform")).get(platform.name());
            if (platformData != null) {
                allPostAnalyses.put("image_posts", platformData);
                totalPostsAnalyzed += ((List<?>) platformData).size();
            }
        }

        if (videoAnalysisOpt.isPresent()) {
            Map<String, Object> videoData = videoAnalysisOpt.get();
            Object platformData = ((Map<String, Object>) videoData.get("results_by_platform")).get(platform.name());
            if (platformData != null) {
                allPostAnalyses.put("video_posts", platformData);
                totalPostsAnalyzed += ((List<?>) platformData).size();
            }
        }

        if (carouselAnalysisOpt.isPresent()) {
            Map<String, Object> carouselData = carouselAnalysisOpt.get();
            Object platformData = ((Map<String, Object>) carouselData.get("results_by_platform")).get(platform.name());
            if (platformData != null) {
                allPostAnalyses.put("carousel_posts", platformData);
                totalPostsAnalyzed += ((List<?>) platformData).size();
            }
        }

        if (threadAnalysisOpt.isPresent()) {
            Map<String, Object> threadData = threadAnalysisOpt.get();
            Object platformData = ((Map<String, Object>) threadData.get("results_by_platform")).get(platform.name());
            if (platformData != null) {
                allPostAnalyses.put("thread_posts", platformData);
                totalPostsAnalyzed += ((List<?>) platformData).size();
            }
        }

        if (!allPostAnalyses.isEmpty()) {
            prompt.append("=== POST ANALYSES BY TYPE ===\n");
            prompt.append(String.format("Total posts analyzed: %d\n\n", totalPostsAnalyzed));
            prompt.append(objectMapper.writeValueAsString(allPostAnalyses)).append("\n\n");
        }

        prompt.append(String.format("""

            Based on ALL the data above, generate a comprehensive %s platform summary:

            {
              "profile_quality": {
                "completeness_score": <1-10>,
                "professional_impression": "<assessment>",
                "brand_clarity": "<assessment>",
                "optimization_level": "<beginner/intermediate/advanced/expert>"
              },
              "content_patterns": {
                "posting_frequency": "<assessment with specific patterns>",
                "content_mix": {
                  "text": "<percentage or count>",
                  "image": "<percentage or count>",
                  "video": "<percentage or count>",
                  "carousel": "<percentage or count>",
                  "thread": "<percentage or count>"
                },
                "best_performing_types": ["<type1>", "<type2>"],
                "posting_consistency": "<assessment>",
                "content_themes": ["<theme1>", "<theme2>", "<theme3>"]
              },
              "writing_style_profile": {
                "dominant_tone": "<tone>",
                "dominant_voice": "<voice>",
                "signature_elements": ["<element1>", "<element2>"],
                "formatting_patterns": ["<pattern1>", "<pattern2>"],
                "personality_traits": ["<trait1>", "<trait2>"],
                "style_consistency_score": <1-10>
              },
              "brand_alignment": {
                "business_consistency": "<how well content aligns with business>",
                "thought_leadership_level": "<beginner/emerging/established/authority>",
                "expertise_demonstration": "<assessment>",
                "brand_voice_strength": <1-10>
              },
              "audience_engagement": {
                "engagement_quality": "<assessment>",
                "engagement_rate_tier": "<low/medium/high/excellent>",
                "community_building": "<assessment>",
                "conversation_starter_ability": <1-10>,
                "audience_relationship_type": "<transactional/growing/engaged/loyal>"
              },
              "competitive_positioning": {
                "differentiation": "<what makes this account unique>",
                "positioning_statement": "<clear statement>",
                "competitive_advantages": ["<advantage1>", "<advantage2>"],
                "gaps_vs_competitors": ["<gap1>", "<gap2>"]
              },
              "recommendations": {
                "content_strategy": ["<recommendation1>", "<recommendation2>"],
                "posting_optimization": ["<recommendation1>", "<recommendation2>"],
                "engagement_tactics": ["<recommendation1>", "<recommendation2>"],
                "profile_improvements": ["<improvement1>", "<improvement2>"],
                "priority_actions": ["<action1>", "<action2>", "<action3>"]
              },
              "platform_specific_insights": {
                "algorithm_optimization": "<assessment of platform algorithm usage>",
                "feature_utilization": ["<feature1>", "<feature2>"],
                "platform_best_practices": "<adherence assessment>"
              }
            }

            Return ONLY the JSON object, no additional text.
            """, platform.getDisplayName()));

        String systemInstruction = String.format("""
            You are an expert %s content strategist and analyst.
            Synthesize ALL available data into actionable insights.
            Be specific, detailed, and provide evidence-based assessments.
            Focus on patterns, consistency, and replicability.
            """, platform.getDisplayName());

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

        Map<String, Object> summaryData = objectMapper.readValue(jsonString.trim(), Map.class);

        // Save to database
        PlatformSummaryEntity entity = PlatformSummaryEntity.builder()
            .platform(fr.postiqa.shared.enums.SocialPlatform.valueOf(platform.name()))
            .totalPostsAnalyzed(totalPostsAnalyzed)
            .profileQuality((Map<String, Object>) summaryData.get("profile_quality"))
            .contentPatterns((Map<String, Object>) summaryData.get("content_patterns"))
            .writingStyleProfile((Map<String, Object>) summaryData.get("writing_style_profile"))
            .brandAlignment((Map<String, Object>) summaryData.get("brand_alignment"))
            .audienceEngagement((Map<String, Object>) summaryData.get("audience_engagement"))
            .competitivePositioning((Map<String, Object>) summaryData.get("competitive_positioning"))
            .recommendations((Map<String, Object>) summaryData.get("recommendations"))
            .build();

        UUID summaryId = analysisAdapter.savePlatformSummary(userProfileAnalysisId, entity);

        log.info("Saved platform summary to database with ID: {}", summaryId);

        // Return complete summary with metadata
        Map<String, Object> result = new HashMap<>(summaryData);
        result.put("platform", platform.name());
        result.put("summary_id", summaryId.toString());
        result.put("total_posts_analyzed", totalPostsAnalyzed);

        return result;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("platform_summaries");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(20); // Large LLM calls with lots of data
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(10));
    }
}
