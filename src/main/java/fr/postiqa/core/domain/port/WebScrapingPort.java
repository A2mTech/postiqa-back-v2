package fr.postiqa.core.domain.port;

import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.model.ScrapingJobResult;
import fr.postiqa.core.domain.model.WebsiteContent;

/**
 * Port for scraping generic website content.
 * <p>
 * Supports scraping various types of web content:
 * - Blog articles
 * - E-commerce product pages
 * - About/profile pages
 * <p>
 * Provides both synchronous and asynchronous execution modes.
 */
public interface WebScrapingPort {

    // ========== SYNCHRONOUS METHODS ==========

    /**
     * Scrape content from a website (synchronous).
     * Blocks until scraping is complete.
     *
     * @param url         the URL to scrape
     * @param contentType the type of content expected
     * @return the scraped website content
     */
    WebsiteContent scrapeWebsite(String url, ContentType contentType);

    // ========== ASYNCHRONOUS NATIVE METHODS (Provider Jobs) ==========

    /**
     * Start an async job to scrape a website (native provider async).
     * Returns immediately with a job ID that can be polled for results.
     *
     * @param url         the URL to scrape
     * @param contentType the type of content expected
     * @return job ID for polling
     */
    String startAsyncWebsiteScraping(String url, ContentType contentType);

    /**
     * Poll the status of an async web scraping job.
     *
     * @param jobId the job ID returned by startAsyncWebsiteScraping
     * @return the current job status with metadata
     */
    ScrapingJobResult<WebsiteContent> pollJob(String jobId);

    /**
     * Get the result of a completed async web scraping job.
     *
     * @param jobId the job ID
     * @return the scraped website content
     * @throws IllegalStateException if job is not complete
     */
    WebsiteContent getJobResult(String jobId);
}
