package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 2B.3: Analyze profile bios for all platforms.
 * <p>
 * Uses LLM to analyze bios/headlines for:
 * - Tone and positioning
 * - Value proposition clarity
 * - Hook effectiveness
 * - Keyword optimization
 * - CTA presence and effectiveness
 */
@Slf4j
@Component
public class AnalyzeBioStep implements WorkflowStep<Void, Map<SocialPlatform, Object>> {

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

    public AnalyzeBioStep(
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
        return "analyze-bios";
    }

    @Override
    public String getStepName() {
        return "Analyze Profile Bios";
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getFromContext(WorkflowContext context, String key, Class<?> type) {
        return (Optional<T>) (Optional<?>) context.get(key, type);
    }

    @Override
    public Map<SocialPlatform, Object> execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);

        Map<SocialPlatform, Object> results = new HashMap<>();

        for (SocialPlatform platform : ALL_PLATFORMS) {
            String profileDataKey = platform.name().toLowerCase() + "_profile_data";
            Optional<Map<String, Object>> profileData = getFromContext(context, profileDataKey, Map.class);

            if (profileData.isEmpty()) {
                continue;
            }

            Map<String, Object> data = profileData.get();
            String bio = (String) data.get("bio");
            String headline = (String) data.get("headline");

            if ((bio == null || bio.isBlank()) && (headline == null || headline.isBlank())) {
                log.debug("Skipping {} - no bio or headline", platform);
                continue;
            }

            log.info("Analyzing bio for platform: {}", platform);

            String prompt = buildBioPrompt(platform, bio, headline);
            String systemInstruction = """
                You are an expert copywriter and personal branding specialist.
                Analyze social media bios/headlines for effectiveness, clarity, and impact.
                Provide actionable insights and specific recommendations for improvement.
                """;

            String response = openAIClient.complete(systemInstruction, prompt);
            Map<String, Object> analysis = parseJsonResponse(response);

            results.put(platform, analysis);

            // Store in context
            String resultKey = platform.name().toLowerCase() + "_bio_analysis";
            context.put(resultKey, analysis);

            log.info("Bio analysis completed for {}", platform);
        }

        log.info("Analyzed bios for {} platforms", results.size());
        return results;
    }

    private String buildBioPrompt(SocialPlatform platform, String bio, String headline) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Analyze this %s profile bio/headline:\n\n", platform.getDisplayName()));

        if (headline != null && !headline.isBlank()) {
            prompt.append("Headline: ").append(headline).append("\n\n");
        }
        if (bio != null && !bio.isBlank()) {
            prompt.append("Bio: ").append(bio).append("\n\n");
        }

        prompt.append("""
            Provide detailed analysis on:

            1. **Tone & Positioning**: What tone is used? How is the person/brand positioned?
            2. **Value Proposition**: Is the value proposition clear? What does this person offer?
            3. **Hook Effectiveness**: Rate the hook/opening line effectiveness (1-10)
            4. **Clarity**: Is the message clear and easy to understand?
            5. **Keywords**: What keywords are used? Are they optimal for discoverability?
            6. **CTA Presence**: Is there a call-to-action? How effective is it?
            7. **Character Count**: Analyze length optimization for the platform
            8. **Authenticity**: Does it feel authentic and genuine?
            9. **Strengths**: What works well?
            10. **Weaknesses**: What could be improved?
            11. **Recommendations**: Specific, actionable suggestions

            Return a structured JSON response with these sections.
            """);

        return prompt.toString();
    }

    private Map<String, Object> parseJsonResponse(String response) throws Exception {
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

        return objectMapper.readValue(jsonString, Map.class);
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("bio_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(8);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(3));
    }
}
