package fr.postiqa.core.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration for in-memory caching using Spring Cache
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SOCIAL_POSTS_CACHE = "socialPosts";
    public static final String SOCIAL_PROFILES_CACHE = "socialProfiles";
    public static final String WEBSITE_CONTENT_CACHE = "websiteContent";
    public static final String SCRAPING_JOBS_CACHE = "scrapingJobs";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache(SOCIAL_POSTS_CACHE),
            new ConcurrentMapCache(SOCIAL_PROFILES_CACHE),
            new ConcurrentMapCache(WEBSITE_CONTENT_CACHE),
            new ConcurrentMapCache(SCRAPING_JOBS_CACHE)
        ));
        return cacheManager;
    }
}
