package fr.postiqa.core.domain.port;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.domain.model.ScrapingJobResult;

import java.util.List;

/**
 * Port for scraping social media posts and profiles.
 * <p>
 * Defines the contract for retrieving posts and profiles from different platforms
 * via external APIs (Apify, Bright Data, etc.).
 * <p>
 * Supports both synchronous and asynchronous (native provider jobs) execution modes.
 */
public interface ScrapingPort {

    // ========== SYNCHRONOUS METHODS ==========

    /**
     * Scrape posts from a user's social media profile (synchronous).
     * Blocks until scraping is complete.
     *
     * @param platform  the social platform to scrape
     * @param userId    the user ID on the platform
     * @param maxPosts  maximum number of posts to retrieve (null = no limit)
     * @return list of scraped posts
     */
    List<SocialPost> scrapePosts(SocialPlatform platform, String userId, Integer maxPosts);

    /**
     * Scrape a user's social media profile information (synchronous).
     * Blocks until scraping is complete.
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @return the scraped profile
     */
    SocialProfile scrapeProfile(SocialPlatform platform, String userId);

    // ========== ASYNCHRONOUS NATIVE METHODS (Provider Jobs) ==========

    /**
     * Start an async job to scrape posts (native provider async).
     * Returns immediately with a job ID that can be polled for results.
     *
     * @param platform  the social platform to scrape
     * @param userId    the user ID on the platform
     * @param maxPosts  maximum number of posts to retrieve (null = no limit)
     * @return job ID for polling
     */
    String startAsyncPostsScraping(SocialPlatform platform, String userId, Integer maxPosts);

    /**
     * Start an async job to scrape a profile (native provider async).
     * Returns immediately with a job ID that can be polled for results.
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @return job ID for polling
     */
    String startAsyncProfileScraping(SocialPlatform platform, String userId);

    /**
     * Poll the status of an async scraping job.
     *
     * @param jobId the job ID returned by startAsync* methods
     * @return the current job status with metadata
     */
    <T> ScrapingJobResult<T> pollJob(String jobId);

    /**
     * Get the result of a completed async job.
     * Throws an exception if job is not complete or failed.
     *
     * @param jobId      the job ID
     * @param resultType the expected result type class
     * @return the job result
     * @throws IllegalStateException if job is not complete
     */
    <T> T getJobResult(String jobId, Class<T> resultType);

    /**
     * Cancel a running async job.
     *
     * @param jobId the job ID to cancel
     */
    void cancelJob(String jobId);
}
