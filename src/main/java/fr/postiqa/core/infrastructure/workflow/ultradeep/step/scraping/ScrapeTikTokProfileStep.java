package fr.postiqa.core.infrastructure.workflow.ultradeep.step.scraping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.domain.port.ScrapingPort;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Step 1.10: Scrape TikTok profile data.
 */
@Slf4j
@Component
public class ScrapeTikTokProfileStep implements WorkflowStep<Void, Map<String, Object>> {

    private final ScrapingPort scrapingPort;
    private final ObjectMapper objectMapper;

    public ScrapeTikTokProfileStep(ScrapingPort scrapingPort, ObjectMapper objectMapper) {
        this.scrapingPort = scrapingPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "scrape-tiktok-profile";
    }

    @Override
    public String getStepName() {
        return "Scrape TikTok Profile";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        String tiktokProfileUrl = context.getRequired("tiktok_profile_url", String.class);
        log.info("Scraping TikTok profile: {}", tiktokProfileUrl);

        SocialProfile profile = scrapingPort.scrapeProfile(
            SocialPlatform.TIKTOK,
            tiktokProfileUrl
        );

        // Convert to Map for context storage
        @SuppressWarnings("unchecked")
        Map<String, Object> profileData = objectMapper.convertValue(profile, Map.class);

        log.info("TikTok profile scraping completed");
        return profileData;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("tiktok_profile_data");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(5);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(3, Duration.ofSeconds(5));
    }

    @Override
    public boolean shouldSkip(WorkflowContext context) {
        return context.get("tiktok_profile_url", String.class).isEmpty();
    }
}
