package fr.postiqa.core.domain.workflow.model;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface for defining a workflow step.
 * Implements Command Pattern - each step is an executable command.
 *
 * @param <I> Input type for the step
 * @param <O> Output type for the step
 */
public interface WorkflowStep<I, O> {

    /**
     * Execute the step logic
     *
     * @param input The input data for this step
     * @param context The shared workflow context
     * @return The output of the step
     * @throws Exception if step execution fails
     */
    O execute(I input, WorkflowContext context) throws Exception;

    /**
     * Get the unique identifier for this step
     */
    String getStepId();

    /**
     * Get the display name for this step
     */
    default String getStepName() {
        return getStepId();
    }

    /**
     * Get the retry policy for this step
     * Default: no retry
     */
    default RetryPolicy getRetryPolicy() {
        return RetryPolicy.none();
    }

    /**
     * Get the timeout for this step
     * Default: 5 minutes
     */
    default Duration getTimeout() {
        return Duration.ofMinutes(5);
    }

    /**
     * Get the compensation action for this step
     * Default: no compensation
     */
    default Optional<CompensationAction<O>> getCompensationAction() {
        return Optional.empty();
    }

    /**
     * Validate input before execution (Template Method hook)
     * Default: no validation
     *
     * @param input The input to validate
     * @throws IllegalArgumentException if validation fails
     */
    default void validateInput(I input) throws IllegalArgumentException {
        // No validation by default
    }

    /**
     * Hook called before step execution (Template Method hook)
     * Useful for logging, metrics, etc.
     */
    default void onBeforeExecute(I input, WorkflowContext context) {
        // No-op by default
    }

    /**
     * Hook called after successful step execution (Template Method hook)
     */
    default void onAfterExecute(O output, WorkflowContext context) {
        // No-op by default
    }

    /**
     * Hook called when step execution fails (Template Method hook)
     */
    default void onError(Exception error, WorkflowContext context) {
        // No-op by default
    }

    /**
     * Determine if this step should be skipped based on context
     * Default: never skip
     */
    default boolean shouldSkip(WorkflowContext context) {
        return false;
    }

    /**
     * Get the expected input key from context (if input comes from context)
     */
    default Optional<String> getInputKey() {
        return Optional.empty();
    }

    /**
     * Get the output key to store in context (if output should be stored)
     */
    default Optional<String> getOutputKey() {
        return Optional.empty();
    }
}
