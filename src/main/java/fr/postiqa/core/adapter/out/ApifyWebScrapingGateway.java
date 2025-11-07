package fr.postiqa.core.adapter.out;

import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.enums.JobStatus;
import fr.postiqa.core.domain.model.ScrapingJobResult;
import fr.postiqa.core.domain.model.WebsiteContent;
import fr.postiqa.core.domain.port.WebScrapingPort;
import fr.postiqa.core.infrastructure.client.ApifyClient;
import fr.postiqa.core.infrastructure.client.dto.ApifyRunResponse;
import fr.postiqa.core.infrastructure.client.dto.ApifyRunStatus;
import fr.postiqa.core.infrastructure.config.ApifyProperties;
import fr.postiqa.core.infrastructure.exception.JobNotCompleteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Apify implementation of WebScrapingPort
 * <p>
 * Handles generic website scraping using Apify web scrapers.
 */
@Component
public class ApifyWebScrapingGateway implements WebScrapingPort {

    private static final Logger log = LoggerFactory.getLogger(ApifyWebScrapingGateway.class);

    private final ApifyClient apifyClient;
    private final ApifyProperties properties;

    public ApifyWebScrapingGateway(ApifyClient apifyClient, ApifyProperties properties) {
        this.apifyClient = apifyClient;
        this.properties = properties;
    }

    @Override
    public WebsiteContent scrapeWebsite(String url, ContentType contentType) {
        log.info("Scraping website: {} (type: {})", url, contentType);

        String actorId = getActorIdForContentType(contentType);
        Map<String, Object> input = buildWebScrapingInput(url, contentType);

        // Start run and wait for completion
        ApifyRunResponse run = apifyClient.startRun(actorId, input);
        ApifyRunStatus status = apifyClient.waitForCompletion(
            run.id(),
            properties.getDefaultTimeout(),
            properties.getPollingInterval()
        );

        if (!status.toJobStatus().isSuccess()) {
            throw new RuntimeException("Web scraping failed with status: " + status.status());
        }

        // Fetch and parse results
        List<Map<String, Object>> items = apifyClient.getRunDataset(run.id());
        if (items.isEmpty()) {
            throw new RuntimeException("No content found at URL: " + url);
        }

        return parseWebsiteContent(items.get(0), url, contentType);
    }

    @Override
    public String startAsyncWebsiteScraping(String url, ContentType contentType) {
        log.info("Starting async scraping of website: {} (type: {})", url, contentType);

        String actorId = getActorIdForContentType(contentType);
        Map<String, Object> input = buildWebScrapingInput(url, contentType);

        ApifyRunResponse run = apifyClient.startRun(actorId, input);
        return run.id();
    }

    @Override
    public ScrapingJobResult<WebsiteContent> pollJob(String jobId) {
        log.debug("Polling web scraping job: {}", jobId);

        ApifyRunStatus status = apifyClient.getRunStatus(jobId);
        JobStatus jobStatus = status.toJobStatus();

        if (!jobStatus.isTerminal()) {
            return ScrapingJobResult.running(jobId, status.startedAt());
        }

        if (jobStatus.isFailure()) {
            String errorMessage = status.exitCode() != null && status.exitCode() != 0
                ? "Exit code: " + status.exitCode()
                : "Unknown error";
            return ScrapingJobResult.failure(jobId, errorMessage, status.startedAt(), status.finishedAt());
        }

        // Success - return status without fetching data
        return new ScrapingJobResult<>(
            jobId,
            jobStatus,
            null, // Result fetched separately via getJobResult
            null,
            status.startedAt(),
            status.finishedAt(),
            Map.of("datasetId", status.defaultDatasetId())
        );
    }

    @Override
    public WebsiteContent getJobResult(String jobId) {
        log.info("Fetching web scraping result for job: {}", jobId);

        ApifyRunStatus status = apifyClient.getRunStatus(jobId);
        JobStatus jobStatus = status.toJobStatus();

        if (!jobStatus.isSuccess()) {
            throw new JobNotCompleteException(jobId, jobStatus);
        }

        List<Map<String, Object>> items = apifyClient.getRunDataset(jobId);
        if (items.isEmpty()) {
            throw new RuntimeException("No content found in job result");
        }

        // Extract URL and content type from job metadata (simplified - should be stored properly)
        String url = (String) items.get(0).get("url");
        ContentType contentType = ContentType.BLOG_ARTICLE; // Should be retrieved from job metadata

        return parseWebsiteContent(items.get(0), url, contentType);
    }

    // ========== Private Helper Methods ==========

    private String getActorIdForContentType(ContentType contentType) {
        // Use different actors based on content type, or default web scraper
        return properties.getWebActorId(contentType.name().toLowerCase());
    }

    private Map<String, Object> buildWebScrapingInput(String url, ContentType contentType) {
        // Build input based on content type
        return switch (contentType) {
            case BLOG_ARTICLE -> Map.of(
                "startUrls", List.of(Map.of("url", url)),
                "selector", "article, .post-content, .entry-content",
                "extractTitle", true,
                "extractImages", true,
                "extractMetadata", true
            );
            case PRODUCT_PAGE -> Map.of(
                "startUrls", List.of(Map.of("url", url)),
                "selector", ".product, .product-details",
                "extractTitle", true,
                "extractPrice", true,
                "extractImages", true,
                "extractReviews", true
            );
            case ABOUT_PAGE -> Map.of(
                "startUrls", List.of(Map.of("url", url)),
                "selector", ".about, .company-info, main",
                "extractTitle", true,
                "extractImages", true,
                "extractMetadata", true
            );
        };
    }

    private WebsiteContent parseWebsiteContent(Map<String, Object> item, String url, ContentType type) {
        return new WebsiteContent(
            url,
            type,
            (String) item.get("title"),
            (String) item.get("mainContent"),
            (String) item.get("excerpt"),
            parseImageUrls(item.get("imageUrls")),
            (String) item.get("author"),
            parseDateTime(item.get("publishedDate")),
            parseMetadata(item),
            LocalDateTime.now()
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> parseImageUrls(Object value) {
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of();
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", value);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(Map<String, Object> item) {
        Object metadata = item.get("metadata");
        if (metadata instanceof Map) {
            return (Map<String, String>) metadata;
        }
        return Map.of();
    }
}
