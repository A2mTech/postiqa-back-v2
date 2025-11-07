package fr.postiqa.core.usecase;

import fr.postiqa.core.domain.model.ScrapingJobResult;
import fr.postiqa.core.domain.port.ScrapingPort;
import fr.postiqa.core.domain.port.WebScrapingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Use case for polling and managing async scraping jobs
 * <p>
 * Handles polling job status and retrieving results for ASYNC_NATIVE execution mode.
 */
@Component
public class PollScrapingJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(PollScrapingJobUseCase.class);

    private final ScrapingPort scrapingPort;
    private final WebScrapingPort webScrapingPort;

    public PollScrapingJobUseCase(ScrapingPort scrapingPort, WebScrapingPort webScrapingPort) {
        this.scrapingPort = scrapingPort;
        this.webScrapingPort = webScrapingPort;
    }

    /**
     * Poll the status of a social media scraping job
     */
    public <T> ScrapingJobResult<T> pollSocialJob(String jobId) {
        log.debug("Polling social scraping job: {}", jobId);
        return scrapingPort.pollJob(jobId);
    }

    /**
     * Poll the status of a web scraping job
     */
    public <T> ScrapingJobResult<T> pollWebJob(String jobId) {
        log.debug("Polling web scraping job: {}", jobId);
        return (ScrapingJobResult<T>) webScrapingPort.pollJob(jobId);
    }

    /**
     * Get the result of a completed social media scraping job
     */
    public <T> T getSocialJobResult(String jobId, Class<T> resultType) {
        log.info("Fetching result for social scraping job: {}", jobId);
        return scrapingPort.getJobResult(jobId, resultType);
    }

    /**
     * Get the result of a completed web scraping job
     */
    public Object getWebJobResult(String jobId) {
        log.info("Fetching result for web scraping job: {}", jobId);
        return webScrapingPort.getJobResult(jobId);
    }

    /**
     * Cancel a running social media scraping job
     */
    public void cancelSocialJob(String jobId) {
        log.info("Cancelling social scraping job: {}", jobId);
        scrapingPort.cancelJob(jobId);
    }

    /**
     * Generic poll method - auto-detects job type
     * (In production, store job type in a registry/map)
     */
    public <T> ScrapingJobResult<T> pollJob(String jobId) {
        // For now, try social first
        // In production: maintain a job registry to track job types
        try {
            return pollSocialJob(jobId);
        } catch (Exception e) {
            log.debug("Job not found in social scraping, trying web scraping");
            return pollWebJob(jobId);
        }
    }
}
