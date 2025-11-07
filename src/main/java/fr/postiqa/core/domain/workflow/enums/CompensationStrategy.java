package fr.postiqa.core.domain.workflow.enums;

/**
 * Defines the compensation (rollback) strategy for a workflow.
 */
public enum CompensationStrategy {
    /**
     * No compensation is performed
     */
    NONE,

    /**
     * Compensate completed steps in reverse order of execution
     * (LIFO - Last In First Out, like a transaction rollback)
     */
    REVERSE_ORDER,

    /**
     * Use custom compensation logic defined by the workflow
     */
    CUSTOM;

    /**
     * Check if compensation should be performed
     */
    public boolean shouldCompensate() {
        return this != NONE;
    }
}
