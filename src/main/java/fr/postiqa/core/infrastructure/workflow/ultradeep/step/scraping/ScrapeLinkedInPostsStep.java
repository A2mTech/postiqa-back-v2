package fr.postiqa.core.infrastructure.workflow.ultradeep.step.scraping;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.port.ScrapingPort;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Step 1.3: Scrape LinkedIn posts.
 * <p>
 * Extracts recent posts (text, images, carousels, videos, engagement metrics).
 */
@Slf4j
@Component
public class ScrapeLinkedInPostsStep implements WorkflowStep<Void, List<SocialPost>> {

    private final ScrapingPort scrapingPort;

    public ScrapeLinkedInPostsStep(ScrapingPort scrapingPort) {
        this.scrapingPort = scrapingPort;
    }

    @Override
    public String getStepId() {
        return "scrape-linkedin-posts";
    }

    @Override
    public String getStepName() {
        return "Scrape LinkedIn Posts";
    }

    @Override
    public List<SocialPost> execute(Void input, WorkflowContext context) throws Exception {
        String linkedinProfileUrl = context.getRequired("linkedin_profile_url", String.class);
        log.info("Scraping LinkedIn posts for profile: {}", linkedinProfileUrl);

        List<SocialPost> posts = scrapingPort.scrapePosts(
            SocialPlatform.LINKEDIN,
            linkedinProfileUrl,
            50 // Last 50 posts
        );

        log.info("LinkedIn posts scraping completed. Posts found: {}", posts.size());
        return posts;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("linkedin_posts");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(15);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(3, Duration.ofSeconds(5));
    }

    @Override
    public boolean shouldSkip(WorkflowContext context) {
        return context.get("linkedin_profile_url", String.class).isEmpty();
    }
}
