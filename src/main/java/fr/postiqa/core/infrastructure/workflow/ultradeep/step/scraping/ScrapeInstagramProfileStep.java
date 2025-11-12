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
 * Step 1.6: Scrape Instagram profile data.
 */
@Slf4j
@Component
public class ScrapeInstagramProfileStep implements WorkflowStep<Void, Map<String, Object>> {

    private final ScrapingPort scrapingPort;
    private final ObjectMapper objectMapper;

    public ScrapeInstagramProfileStep(ScrapingPort scrapingPort, ObjectMapper objectMapper) {
        this.scrapingPort = scrapingPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "scrape-instagram-profile";
    }

    @Override
    public String getStepName() {
        return "Scrape Instagram Profile";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        String instagramProfileUrl = context.getRequired("instagram_profile_url", String.class);
        log.info("Scraping Instagram profile: {}", instagramProfileUrl);

        SocialProfile profile = scrapingPort.scrapeProfile(
            SocialPlatform.INSTAGRAM,
            instagramProfileUrl
        );

        // Convert to Map for context storage
        @SuppressWarnings("unchecked")
        Map<String, Object> profileData = objectMapper.convertValue(profile, Map.class);

        log.info("Instagram profile scraping completed");
        return profileData;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("instagram_profile_data");
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
        return context.get("instagram_profile_url", String.class).isEmpty();
    }
}
