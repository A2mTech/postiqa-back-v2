package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.analysis.ProfileAnalysisResult;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.GPT4VisionClient;
import fr.postiqa.database.entity.PlatformProfileAnalysisEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 2B.1: Analyze profile pictures for all platforms.
 * <p>
 * Uses GPT-4 Vision to analyze profile pictures for:
 * - Visual identity and professionalism
 * - Brand alignment
 * - First impression impact
 * - Consistency across platforms
 */
@Slf4j
@Component
public class AnalyzeProfilePictureStep implements WorkflowStep<Void, Map<SocialPlatform, Object>> {

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

    public AnalyzeProfilePictureStep(
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
        return "analyze-profile-pictures";
    }

    @Override
    public String getStepName() {
        return "Analyze Profile Pictures";
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
                log.debug("Skipping {} - no profile data found", platform);
                continue;
            }

            Map<String, Object> data = profileData.get();
            String profilePictureUrl = (String) data.get("profile_picture_url");

            if (profilePictureUrl == null || profilePictureUrl.isBlank()) {
                log.debug("Skipping {} - no profile picture URL", platform);
                continue;
            }

            log.info("Analyzing profile picture for platform: {}", platform);

            String prompt = buildProfilePicturePrompt(platform);
            Map<String, Object> analysis = visionClient.analyzeImage(profilePictureUrl, prompt);

            results.put(platform, analysis);

            // Store in context for later platform profile aggregation
            String resultKey = platform.name().toLowerCase() + "_profile_picture_analysis";
            context.put(resultKey, analysis);

            log.info("Profile picture analysis completed for {}", platform);
        }

        log.info("Analyzed profile pictures for {} platforms", results.size());
        return results;
    }

    private String buildProfilePicturePrompt(SocialPlatform platform) {
        return String.format("""
            Analyze this %s profile picture and provide detailed insights:

            1. **Visual Identity**: Describe the subject, composition, and overall impression
            2. **Professionalism**: Rate the professionalism level (1-10) and explain
            3. **Brand Alignment**: How well does it align with personal/business branding?
            4. **First Impression**: What's the immediate impact and message conveyed?
            5. **Platform Appropriateness**: Is it suitable for %s? Why or why not?
            6. **Authenticity**: Does it feel authentic and genuine?
            7. **Visual Quality**: Technical quality (lighting, resolution, framing)
            8. **Recommendations**: Specific suggestions for improvement

            Return a structured JSON response with these sections.
            """, platform.getDisplayName(), platform.getDisplayName());
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("profile_picture_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(10); // Multiple platforms
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(3));
    }
}
