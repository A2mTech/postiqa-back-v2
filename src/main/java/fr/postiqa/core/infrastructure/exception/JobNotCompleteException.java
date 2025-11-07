package fr.postiqa.core.infrastructure.exception;

import fr.postiqa.core.domain.enums.JobStatus;

/**
 * Exception thrown when trying to retrieve results from an incomplete job
 */
public class JobNotCompleteException extends ScrapingException {

    private final String jobId;
    private final JobStatus currentStatus;

    public JobNotCompleteException(String jobId, JobStatus currentStatus) {
        super(String.format("Job '%s' is not complete. Current status: %s", jobId, currentStatus));
        this.jobId = jobId;
        this.currentStatus = currentStatus;
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getCurrentStatus() {
        return currentStatus;
    }
}
