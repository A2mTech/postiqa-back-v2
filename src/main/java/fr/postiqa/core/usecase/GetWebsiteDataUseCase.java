package fr.postiqa.core.usecase;

import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.model.WebsiteContent;
import fr.postiqa.core.domain.port.CachePort;
import fr.postiqa.core.domain.port.WebScrapingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Use case for retrieving website content
 * <p>
 * Supports three execution modes:
 * 1. SYNC - Blocking call, returns immediately with results
 * 2. ASYNC_THREAD - Non-blocking, returns CompletableFuture
 * 3. ASYNC_NATIVE - Returns job ID for polling
 */
@Component
public class GetWebsiteDataUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetWebsiteDataUseCase.class);
    private static final Duration CACHE_TTL = Duration.ofHours(6); // Website content changes less frequently

    private final WebScrapingPort webScrapingPort;
    private final CachePort cachePort;

    public GetWebsiteDataUseCase(WebScrapingPort webScrapingPort, CachePort cachePort) {
        this.webScrapingPort = webScrapingPort;
        this.cachePort = cachePort;
    }

    /**
     * Execute synchronously - blocks until result is available
     */
    public WebsiteContent execute(String url, ContentType contentType) {
        log.info("Fetching website data (SYNC): {} (type: {})", url, contentType);

        String cacheKey = buildCacheKey(url, contentType);

        // Check cache first
        return cachePort.get(cacheKey, WebsiteContent.class)
            .orElseGet(() -> {
                // Cache miss - scrape from provider
                WebsiteContent content = webScrapingPort.scrapeWebsite(url, contentType);

                // Cache the result
                cachePort.put(cacheKey, content, CACHE_TTL);

                return content;
            });
    }

    /**
     * Execute asynchronously in a separate thread using @Async
     */
    @Async("scrapingExecutor")
    public CompletableFuture<WebsiteContent> executeAsync(String url, ContentType contentType) {
        log.info("Fetching website data (ASYNC_THREAD): {} (type: {})", url, contentType);

        return CompletableFuture.supplyAsync(() -> execute(url, contentType));
    }

    /**
     * Start an async native job (provider-side async)
     * Returns job ID for polling
     */
    public String startJob(String url, ContentType contentType) {
        log.info("Starting async job (ASYNC_NATIVE): {} (type: {})", url, contentType);

        return webScrapingPort.startAsyncWebsiteScraping(url, contentType);
    }

    private String buildCacheKey(String url, ContentType contentType) {
        return String.format("website:%s:%s", contentType, url);
    }
}
