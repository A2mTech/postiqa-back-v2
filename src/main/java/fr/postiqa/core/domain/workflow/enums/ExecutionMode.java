package fr.postiqa.core.domain.workflow.enums;

/**
 * Defines how steps should be executed in a workflow.
 */
public enum ExecutionMode {
    /**
     * Steps are executed one after another in sequence
     */
    SEQUENTIAL,

    /**
     * Steps with no dependencies are executed concurrently
     */
    PARALLEL;

    /**
     * Check if execution mode allows parallel execution
     */
    public boolean allowsParallelism() {
        return this == PARALLEL;
    }
}
