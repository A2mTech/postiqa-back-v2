package fr.postiqa.core.domain.workflow.enums;

/**
 * Represents the overall status of a workflow instance.
 * Used to track the lifecycle state of workflow execution.
 */
public enum WorkflowStatus {
    /**
     * Workflow has been created but not yet started
     */
    PENDING,

    /**
     * Workflow is currently executing steps
     */
    RUNNING,

    /**
     * Workflow execution has been paused and can be resumed
     */
    PAUSED,

    /**
     * All workflow steps completed successfully
     */
    COMPLETED,

    /**
     * Workflow execution failed and cannot continue
     */
    FAILED,

    /**
     * Workflow is in the process of compensation (rollback)
     */
    COMPENSATING,

    /**
     * Workflow compensation completed successfully
     */
    COMPENSATED,

    /**
     * Workflow was explicitly cancelled by user or system
     */
    CANCELLED;

    /**
     * Check if workflow is in a terminal state (cannot transition further)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == COMPENSATED || this == CANCELLED;
    }

    /**
     * Check if workflow can be resumed
     */
    public boolean canResume() {
        return this == PAUSED;
    }

    /**
     * Check if workflow can be paused
     */
    public boolean canPause() {
        return this == RUNNING;
    }

    /**
     * Check if workflow can be compensated
     */
    public boolean canCompensate() {
        return this == FAILED;
    }
}
