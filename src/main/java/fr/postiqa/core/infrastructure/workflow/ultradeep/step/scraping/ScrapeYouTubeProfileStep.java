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
 * Step 1.8: Scrape YouTube channel profile data.
 */
@Slf4j
@Component
public class ScrapeYouTubeProfileStep implements WorkflowStep<Void, Map<String, Object>> {

    private final ScrapingPort scrapingPort;
    private final ObjectMapper objectMapper;

    public ScrapeYouTubeProfileStep(ScrapingPort scrapingPort, ObjectMapper objectMapper) {
        this.scrapingPort = scrapingPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "scrape-youtube-profile";
    }

    @Override
    public String getStepName() {
        return "Scrape YouTube Profile";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        String youtubeProfileUrl = context.getRequired("youtube_profile_url", String.class);
        log.info("Scraping YouTube profile: {}", youtubeProfileUrl);

        SocialProfile profile = scrapingPort.scrapeProfile(
            SocialPlatform.YOUTUBE,
            youtubeProfileUrl
        );

        // Convert to Map for context storage
        @SuppressWarnings("unchecked")
        Map<String, Object> profileData = objectMapper.convertValue(profile, Map.class);

        log.info("YouTube profile scraping completed");
        return profileData;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("youtube_profile_data");
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
        return context.get("youtube_profile_url", String.class).isEmpty();
    }
}
