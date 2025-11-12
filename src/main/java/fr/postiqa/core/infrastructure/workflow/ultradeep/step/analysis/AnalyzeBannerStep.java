package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.GPT4VisionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 2B.2: Analyze profile banners for all platforms.
 * <p>
 * Uses GPT-4 Vision to analyze banners/headers for:
 * - Brand coherence and messaging
 * - Visual impact and design quality
 * - Value proposition communication
 * - Consistency across platforms
 */
@Slf4j
@Component
public class AnalyzeBannerStep implements WorkflowStep<Void, Map<SocialPlatform, Object>> {

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

    public AnalyzeBannerStep(
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
        return "analyze-banners";
    }

    @Override
    public String getStepName() {
        return "Analyze Profile Banners";
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
            String bannerUrl = (String) data.get("banner_url");

            if (bannerUrl == null || bannerUrl.isBlank()) {
                log.debug("Skipping {} - no banner URL", platform);
                continue;
            }

            log.info("Analyzing banner for platform: {}", platform);

            String prompt = buildBannerPrompt(platform);
            Map<String, Object> analysis = visionClient.analyzeImage(bannerUrl, prompt);

            results.put(platform, analysis);

            // Store in context
            String resultKey = platform.name().toLowerCase() + "_banner_analysis";
            context.put(resultKey, analysis);

            log.info("Banner analysis completed for {}", platform);
        }

        log.info("Analyzed banners for {} platforms", results.size());
        return results;
    }

    private String buildBannerPrompt(SocialPlatform platform) {
        return String.format("""
            Analyze this %s profile banner/header and provide detailed insights:

            1. **Visual Design**: Describe the design, colors, composition, and style
            2. **Brand Coherence**: How well does it communicate brand identity?
            3. **Messaging**: What's the primary message or value proposition conveyed?
            4. **Visual Impact**: Rate the visual impact and memorability (1-10)
            5. **Professional Quality**: Technical and design quality assessment
            6. **Platform Optimization**: Is it optimized for %s specifications?
            7. **Call-to-Action**: Are there any CTAs or key information displayed?
            8. **Consistency**: Brand consistency elements (colors, fonts, imagery)
            9. **Recommendations**: Specific suggestions for improvement

            Return a structured JSON response with these sections.
            """, platform.getDisplayName(), platform.getDisplayName());
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("banner_analyses");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(10);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(3));
    }
}
