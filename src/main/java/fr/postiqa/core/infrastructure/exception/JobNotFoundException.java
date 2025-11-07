package fr.postiqa.core.infrastructure.exception;

/**
 * Exception thrown when an async job cannot be found
 */
public class JobNotFoundException extends ScrapingException {

    private final String jobId;

    public JobNotFoundException(String jobId) {
        super(String.format("Scraping job not found: %s", jobId));
        this.jobId = jobId;
    }

    public JobNotFoundException(String jobId, String message) {
        super(String.format("Scraping job '%s' not found: %s", jobId, message));
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }
}
