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
 * Step 1.2: Scrape LinkedIn profile data.
 * <p>
 * Extracts: profile picture, banner, bio, headline, experience, education, skills, etc.
 */
@Slf4j
@Component
public class ScrapeLinkedInProfileStep implements WorkflowStep<Void, Map<String, Object>> {

    private final ScrapingPort scrapingPort;
    private final ObjectMapper objectMapper;

    public ScrapeLinkedInProfileStep(ScrapingPort scrapingPort, ObjectMapper objectMapper) {
        this.scrapingPort = scrapingPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "scrape-linkedin-profile";
    }

    @Override
    public String getStepName() {
        return "Scrape LinkedIn Profile";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        String linkedinProfileUrl = context.getRequired("linkedin_profile_url", String.class);
        log.info("Scraping LinkedIn profile: {}", linkedinProfileUrl);

        SocialProfile profile = scrapingPort.scrapeProfile(
            SocialPlatform.LINKEDIN,
            linkedinProfileUrl
        );

        // Convert to Map for context storage
        @SuppressWarnings("unchecked")
        Map<String, Object> profileData = objectMapper.convertValue(profile, Map.class);

        log.info("LinkedIn profile scraping completed");
        return profileData;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("linkedin_profile_data");
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
        // Skip if LinkedIn profile URL is not provided
        return context.get("linkedin_profile_url", String.class).isEmpty();
    }
}
