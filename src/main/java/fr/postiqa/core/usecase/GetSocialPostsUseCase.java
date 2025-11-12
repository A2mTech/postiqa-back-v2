package fr.postiqa.core.usecase;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.port.CachePort;
import fr.postiqa.core.domain.port.ScrapingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Use case for retrieving social media posts
 * <p>
 * Supports three execution modes:
 * 1. SYNC - Blocking call, returns immediately with results
 * 2. ASYNC_THREAD - Non-blocking, returns CompletableFuture
 * 3. ASYNC_NATIVE - Returns job ID for polling
 */
@fr.postiqa.shared.annotation.UseCase(
    value = "GetSocialPosts",
    resourceType = "SOCIAL_POST",
    description = "Retrieves social media posts from platforms",
    logActivity = false  // Read-only operation
)
public class GetSocialPostsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSocialPostsUseCase.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final ScrapingPort scrapingPort;
    private final CachePort cachePort;

    public GetSocialPostsUseCase(ScrapingPort scrapingPort, CachePort cachePort) {
        this.scrapingPort = scrapingPort;
        this.cachePort = cachePort;
    }

    /**
     * Execute synchronously - blocks until results are available
     */
    public List<SocialPost> execute(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("Fetching posts (SYNC): {} - {} (max: {})", platform, userId, maxPosts);

        String cacheKey = buildCacheKey(platform, userId, maxPosts);

        // Check cache first
        return cachePort.get(cacheKey, List.class)
            .orElseGet(() -> {
                // Cache miss - scrape from provider
                List<SocialPost> posts = scrapingPort.scrapePosts(platform, userId, maxPosts);

                // Cache the result
                cachePort.put(cacheKey, posts, CACHE_TTL);

                return posts;
            });
    }

    /**
     * Execute asynchronously in a separate thread using @Async
     */
    @Async("scrapingExecutor")
    public CompletableFuture<List<SocialPost>> executeAsync(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("Fetching posts (ASYNC_THREAD): {} - {} (max: {})", platform, userId, maxPosts);

        return CompletableFuture.supplyAsync(() -> execute(platform, userId, maxPosts));
    }

    /**
     * Start an async native job (provider-side async)
     * Returns job ID for polling
     */
    public String startJob(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("Starting async job (ASYNC_NATIVE): {} - {} (max: {})", platform, userId, maxPosts);

        return scrapingPort.startAsyncPostsScraping(platform, userId, maxPosts);
    }

    private String buildCacheKey(SocialPlatform platform, String userId, Integer maxPosts) {
        return String.format("posts:%s:%s:%d", platform, userId, maxPosts != null ? maxPosts : 0);
    }
}
