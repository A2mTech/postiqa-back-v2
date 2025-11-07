package fr.postiqa.core.domain.workflow.enums;

/**
 * Represents the status of an individual workflow step execution.
 */
public enum StepStatus {
    /**
     * Step is waiting to be executed (dependencies not met or queued)
     */
    PENDING,

    /**
     * Step is currently executing
     */
    RUNNING,

    /**
     * Step completed successfully
     */
    COMPLETED,

    /**
     * Step execution failed
     */
    FAILED,

    /**
     * Step is retrying after a failure
     */
    RETRYING,

    /**
     * Step was skipped (e.g., conditional execution)
     */
    SKIPPED,

    /**
     * Step was compensated (rolled back)
     */
    COMPENSATED,

    /**
     * Step compensation is in progress
     */
    COMPENSATING,

    /**
     * Step execution timed out
     */
    TIMED_OUT;

    /**
     * Check if step is in a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == SKIPPED ||
               this == COMPENSATED || this == TIMED_OUT;
    }

    /**
     * Check if step can be retried
     */
    public boolean canRetry() {
        return this == FAILED || this == TIMED_OUT;
    }

    /**
     * Check if step can be compensated
     */
    public boolean canCompensate() {
        return this == COMPLETED;
    }
}
