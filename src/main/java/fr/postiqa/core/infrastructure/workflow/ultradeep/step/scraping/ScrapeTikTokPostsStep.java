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
 * Step 1.11: Scrape TikTok videos.
 */
@Slf4j
@Component
public class ScrapeTikTokPostsStep implements WorkflowStep<Void, List<SocialPost>> {

    private final ScrapingPort scrapingPort;

    public ScrapeTikTokPostsStep(ScrapingPort scrapingPort) {
        this.scrapingPort = scrapingPort;
    }

    @Override
    public String getStepId() {
        return "scrape-tiktok-posts";
    }

    @Override
    public String getStepName() {
        return "Scrape TikTok Videos";
    }

    @Override
    public List<SocialPost> execute(Void input, WorkflowContext context) throws Exception {
        String tiktokProfileUrl = context.getRequired("tiktok_profile_url", String.class);
        log.info("Scraping TikTok videos for profile: {}", tiktokProfileUrl);

        List<SocialPost> posts = scrapingPort.scrapePosts(
            SocialPlatform.TIKTOK,
            tiktokProfileUrl,
            50 // Last 50 videos
        );

        log.info("TikTok videos scraping completed. Videos found: {}", posts.size());
        return posts;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("tiktok_posts");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(20); // Videos take longer
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
