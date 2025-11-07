package fr.postiqa.core.domain.enums;

/**
 * Status of an async scraping job
 */
public enum JobStatus {
    /**
     * Job has been created but not yet started
     */
    READY,

    /**
     * Job is currently running
     */
    RUNNING,

    /**
     * Job completed successfully
     */
    SUCCEEDED,

    /**
     * Job failed with an error
     */
    FAILED,

    /**
     * Job was cancelled by user
     */
    CANCELLED,

    /**
     * Job timed out
     */
    TIMED_OUT,

    /**
     * Job was aborted due to system issues
     */
    ABORTED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED || this == TIMED_OUT || this == ABORTED;
    }

    public boolean isSuccess() {
        return this == SUCCEEDED;
    }

    public boolean isFailure() {
        return this == FAILED || this == TIMED_OUT || this == ABORTED;
    }

    public boolean isInProgress() {
        return this == READY || this == RUNNING;
    }
}
