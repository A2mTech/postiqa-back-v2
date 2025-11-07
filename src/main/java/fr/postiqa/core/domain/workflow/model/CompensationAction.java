package fr.postiqa.core.domain.workflow.model;

/**
 * Functional interface for compensation (rollback) actions.
 * Compensation actions are executed in reverse order when a workflow fails
 * to undo the effects of previously completed steps.
 *
 * @param <I> The input type for compensation (typically the output of the original step)
 */
@FunctionalInterface
public interface CompensationAction<I> {

    /**
     * Execute the compensation logic
     *
     * @param input The input data for compensation (usually the step's output)
     * @param context The workflow context at the time of compensation
     * @throws Exception if compensation fails
     */
    void compensate(I input, WorkflowContext context) throws Exception;

    /**
     * Create a no-op compensation action (does nothing)
     */
    static <I> CompensationAction<I> noOp() {
        return (input, context) -> {
            // No-op
        };
    }

    /**
     * Combine this compensation with another (executes both in sequence)
     */
    default CompensationAction<I> andThen(CompensationAction<I> after) {
        return (input, context) -> {
            this.compensate(input, context);
            after.compensate(input, context);
        };
    }
}
