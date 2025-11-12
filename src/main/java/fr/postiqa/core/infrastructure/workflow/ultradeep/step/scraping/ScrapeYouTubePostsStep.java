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
 * Step 1.9: Scrape YouTube videos.
 */
@Slf4j
@Component
public class ScrapeYouTubePostsStep implements WorkflowStep<Void, List<SocialPost>> {

    private final ScrapingPort scrapingPort;

    public ScrapeYouTubePostsStep(ScrapingPort scrapingPort) {
        this.scrapingPort = scrapingPort;
    }

    @Override
    public String getStepId() {
        return "scrape-youtube-posts";
    }

    @Override
    public String getStepName() {
        return "Scrape YouTube Videos";
    }

    @Override
    public List<SocialPost> execute(Void input, WorkflowContext context) throws Exception {
        String youtubeProfileUrl = context.getRequired("youtube_profile_url", String.class);
        log.info("Scraping YouTube videos for channel: {}", youtubeProfileUrl);

        List<SocialPost> posts = scrapingPort.scrapePosts(
            SocialPlatform.YOUTUBE,
            youtubeProfileUrl,
            30 // Last 30 videos
        );

        log.info("YouTube videos scraping completed. Videos found: {}", posts.size());
        return posts;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("youtube_posts");
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
        return context.get("youtube_profile_url", String.class).isEmpty();
    }
}
