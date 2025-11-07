package fr.postiqa.core.adapter.in;

import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.ScrapingJobResult;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.domain.model.WebsiteContent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Public facade for the Core module
 * <p>
 * This is the main API that business and agency modules should use.
 * Provides three execution modes for all operations:
 * 1. SYNC - Blocking methods that return results immediately
 * 2. ASYNC_THREAD - Non-blocking methods returning CompletableFuture
 * 3. ASYNC_NATIVE - Job-based methods for provider-side async execution
 */
public interface CoreFacade {

    // ========== SYNCHRONOUS METHODS ==========

    /**
     * Get social media posts synchronously
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @param maxPosts maximum number of posts to retrieve (null = no limit)
     * @return list of posts
     */
    List<SocialPost> getSocialPosts(SocialPlatform platform, String userId, Integer maxPosts);

    /**
     * Get social media profile synchronously
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @return the user profile
     */
    SocialProfile getSocialProfile(SocialPlatform platform, String userId);

    /**
     * Get website content synchronously
     *
     * @param url         the URL to scrape
     * @param contentType the type of content expected
     * @return the website content
     */
    WebsiteContent getWebsiteData(String url, ContentType contentType);

    // ========== ASYNC THREAD METHODS (CompletableFuture) ==========

    /**
     * Get social media posts asynchronously (separate thread)
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @param maxPosts maximum number of posts to retrieve
     * @return CompletableFuture with list of posts
     */
    CompletableFuture<List<SocialPost>> getSocialPostsAsync(SocialPlatform platform, String userId, Integer maxPosts);

    /**
     * Get social media profile asynchronously (separate thread)
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @return CompletableFuture with profile
     */
    CompletableFuture<SocialProfile> getSocialProfileAsync(SocialPlatform platform, String userId);

    /**
     * Get website content asynchronously (separate thread)
     *
     * @param url         the URL to scrape
     * @param contentType the type of content expected
     * @return CompletableFuture with website content
     */
    CompletableFuture<WebsiteContent> getWebsiteDataAsync(String url, ContentType contentType);

    // ========== ASYNC NATIVE METHODS (Job-based, requires polling) ==========

    /**
     * Start an async job to scrape social media posts
     * Returns immediately with a job ID that can be polled for completion
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @param maxPosts maximum number of posts to retrieve
     * @return job ID for polling
     */
    String startSocialPostsJob(SocialPlatform platform, String userId, Integer maxPosts);

    /**
     * Start an async job to scrape a social media profile
     *
     * @param platform the social platform to scrape
     * @param userId   the user ID on the platform
     * @return job ID for polling
     */
    String startSocialProfileJob(SocialPlatform platform, String userId);

    /**
     * Start an async job to scrape website content
     *
     * @param url         the URL to scrape
     * @param contentType the type of content expected
     * @return job ID for polling
     */
    String startWebsiteDataJob(String url, ContentType contentType);

    /**
     * Poll the status of an async scraping job
     *
     * @param jobId the job ID returned by startXxxJob methods
     * @return current job status with metadata
     */
    <T> ScrapingJobResult<T> pollJob(String jobId);

    /**
     * Get the result of a completed async job
     *
     * @param jobId      the job ID
     * @param resultType the expected result type
     * @return the job result
     * @throws fr.postiqa.core.infrastructure.exception.JobNotCompleteException if job is not complete
     */
    <T> T getJobResult(String jobId, Class<T> resultType);

    /**
     * Cancel a running async job
     *
     * @param jobId the job ID to cancel
     */
    void cancelJob(String jobId);
}
