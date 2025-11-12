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
 * Step 1.5: Scrape Twitter posts.
 */
@Slf4j
@Component
public class ScrapeTwitterPostsStep implements WorkflowStep<Void, List<SocialPost>> {

    private final ScrapingPort scrapingPort;

    public ScrapeTwitterPostsStep(ScrapingPort scrapingPort) {
        this.scrapingPort = scrapingPort;
    }

    @Override
    public String getStepId() {
        return "scrape-twitter-posts";
    }

    @Override
    public String getStepName() {
        return "Scrape Twitter Posts";
    }

    @Override
    public List<SocialPost> execute(Void input, WorkflowContext context) throws Exception {
        String twitterProfileUrl = context.getRequired("twitter_profile_url", String.class);
        log.info("Scraping Twitter posts for profile: {}", twitterProfileUrl);

        List<SocialPost> posts = scrapingPort.scrapePosts(
            SocialPlatform.TWITTER,
            twitterProfileUrl,
            100 // Last 100 tweets
        );

        log.info("Twitter posts scraping completed. Posts found: {}", posts.size());
        return posts;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("twitter_posts");
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
        return context.get("twitter_profile_url", String.class).isEmpty();
    }
}
