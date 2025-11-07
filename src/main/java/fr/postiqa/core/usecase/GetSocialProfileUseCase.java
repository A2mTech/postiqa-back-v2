package fr.postiqa.core.usecase;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.domain.port.CachePort;
import fr.postiqa.core.domain.port.ScrapingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Use case for retrieving social media profiles
 * <p>
 * Supports three execution modes:
 * 1. SYNC - Blocking call, returns immediately with results
 * 2. ASYNC_THREAD - Non-blocking, returns CompletableFuture
 * 3. ASYNC_NATIVE - Returns job ID for polling
 */
@Component
public class GetSocialProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSocialProfileUseCase.class);
    private static final Duration CACHE_TTL = Duration.ofHours(1); // Profiles change less frequently

    private final ScrapingPort scrapingPort;
    private final CachePort cachePort;

    public GetSocialProfileUseCase(ScrapingPort scrapingPort, CachePort cachePort) {
        this.scrapingPort = scrapingPort;
        this.cachePort = cachePort;
    }

    /**
     * Execute synchronously - blocks until result is available
     */
    public SocialProfile execute(SocialPlatform platform, String userId) {
        log.info("Fetching profile (SYNC): {} - {}", platform, userId);

        String cacheKey = buildCacheKey(platform, userId);

        // Check cache first
        return cachePort.get(cacheKey, SocialProfile.class)
            .orElseGet(() -> {
                // Cache miss - scrape from provider
                SocialProfile profile = scrapingPort.scrapeProfile(platform, userId);

                // Cache the result
                cachePort.put(cacheKey, profile, CACHE_TTL);

                return profile;
            });
    }

    /**
     * Execute asynchronously in a separate thread using @Async
     */
    @Async("scrapingExecutor")
    public CompletableFuture<SocialProfile> executeAsync(SocialPlatform platform, String userId) {
        log.info("Fetching profile (ASYNC_THREAD): {} - {}", platform, userId);

        return CompletableFuture.supplyAsync(() -> execute(platform, userId));
    }

    /**
     * Start an async native job (provider-side async)
     * Returns job ID for polling
     */
    public String startJob(SocialPlatform platform, String userId) {
        log.info("Starting async job (ASYNC_NATIVE): {} - {}", platform, userId);

        return scrapingPort.startAsyncProfileScraping(platform, userId);
    }

    private String buildCacheKey(SocialPlatform platform, String userId) {
        return String.format("profile:%s:%s", platform, userId);
    }
}
