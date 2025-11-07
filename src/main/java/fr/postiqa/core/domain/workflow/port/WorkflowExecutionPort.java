package fr.postiqa.core.domain.workflow.port;

import fr.postiqa.core.domain.workflow.model.*;

import java.util.concurrent.CompletableFuture;

/**
 * Port for executing workflow steps.
 * Defines the contract for step execution with retry, timeout, and compensation.
 * This is a secondary port (driven/out) - implemented by infrastructure layer.
 */
public interface WorkflowExecutionPort {

    /**
     * Execute a workflow step with all resilience features (retry, timeout, compensation)
     *
     * @param step The step to execute
     * @param input The input data for the step
     * @param context The workflow context
     * @param <I> Input type
     * @param <O> Output type
     * @return A StepResult containing the execution result
     */
    <I, O> StepResult<O> executeStep(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context
    );

    /**
     * Execute a workflow step asynchronously
     *
     * @param step The step to execute
     * @param input The input data for the step
     * @param context The workflow context
     * @param <I> Input type
     * @param <O> Output type
     * @return A CompletableFuture of StepResult
     */
    <I, O> CompletableFuture<StepResult<O>> executeStepAsync(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context
    );

    /**
     * Execute a compensation action for a step
     *
     * @param compensationAction The compensation action to execute
     * @param input The input data (typically the step's output)
     * @param context The workflow context
     * @param <I> Input type
     * @return true if compensation succeeded, false otherwise
     */
    <I> boolean executeCompensation(
        CompensationAction<I> compensationAction,
        I input,
        WorkflowContext context
    );

    /**
     * Cancel a running step execution
     *
     * @param stepId The ID of the step to cancel
     * @return true if cancellation succeeded, false otherwise
     */
    boolean cancelStep(String stepId);
}
