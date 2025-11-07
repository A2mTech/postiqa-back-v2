package fr.postiqa.core.infrastructure.workflow.engine;

import fr.postiqa.core.domain.workflow.model.*;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import fr.postiqa.core.infrastructure.workflow.resilience.RetryHandler;
import fr.postiqa.core.infrastructure.workflow.resilience.TimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Executes individual workflow steps with retry, timeout, and hooks.
 * Handles async execution and integrates with event publishing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StepExecutor {

    private final RetryHandler retryHandler;
    private final TimeoutHandler timeoutHandler;
    private final WorkflowEventPort eventPort;

    /**
     * Execute a workflow step synchronously with all resilience features
     *
     * @param step The step to execute
     * @param input The input data
     * @param context The workflow context
     * @param instanceId The workflow instance ID
     * @param <I> Input type
     * @param <O> Output type
     * @return StepResult containing execution result
     */
    public <I, O> StepResult<O> executeStep(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context,
        String instanceId
    ) {
        String stepId = step.getStepId();
        log.info("Executing step: {} for workflow instance: {}", stepId, instanceId);

        // Publish start event
        eventPort.publishStepStarted(instanceId, stepId);

        // Check if step should be skipped
        if (step.shouldSkip(context)) {
            log.info("Step {} skipped based on context", stepId);
            eventPort.publishStepSkipped(instanceId, stepId);
            return StepResult.skipped(Instant.now());
        }

        // Validate input
        try {
            step.validateInput(input);
        } catch (IllegalArgumentException e) {
            log.error("Input validation failed for step {}: {}", stepId, e.getMessage());
            return StepResult.failure(e, Instant.now(), Instant.now(), 0);
        }

        // Execute with retry
        try {
            return retryHandler.executeWithRetry(
                () -> executeSingleAttempt(step, input, context, instanceId, 0),
                step.getRetryPolicy(),
                stepId
            );
        } catch (Exception e) {
            // If retry handler gives up, return failure
            Instant now = Instant.now();
            log.error("Step {} failed after all retries: {}", stepId, e.getMessage());
            return StepResult.failure(e, now, now, step.getRetryPolicy().maxAttempts());
        }
    }

    /**
     * Execute a workflow step asynchronously
     *
     * @param step The step to execute
     * @param input The input data
     * @param context The workflow context
     * @param instanceId The workflow instance ID
     * @param <I> Input type
     * @param <O> Output type
     * @return CompletableFuture of StepResult
     */
    @Async("workflowExecutor")
    public <I, O> CompletableFuture<StepResult<O>> executeStepAsync(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context,
        String instanceId
    ) {
        return CompletableFuture.supplyAsync(() ->
            executeStep(step, input, context, instanceId)
        );
    }

    /**
     * Execute a single attempt of a step (with timeout)
     */
    private <I, O> StepResult<O> executeSingleAttempt(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context,
        String instanceId,
        int attemptNumber
    ) {
        String stepId = step.getStepId();
        Instant startTime = Instant.now();

        if (attemptNumber > 0) {
            log.info("Retry attempt {} for step: {}", attemptNumber, stepId);
            eventPort.publishStepRetried(instanceId, stepId, attemptNumber);
        }

        try {
            // Execute before hook
            step.onBeforeExecute(input, context);

            // Execute step with timeout
            O output = timeoutHandler.executeWithTimeout(
                () -> step.execute(input, context),
                step.getTimeout(),
                stepId
            );

            Instant endTime = Instant.now();

            // Execute after hook
            step.onAfterExecute(output, context);

            // Create success result
            StepResult<O> result = StepResult.success(output, startTime, endTime, attemptNumber);

            log.info("Step {} completed successfully in {}ms",
                stepId, result.duration().toMillis());

            eventPort.publishStepCompleted(instanceId, stepId, result);

            return result;

        } catch (TimeoutException e) {
            Instant endTime = Instant.now();
            log.error("Step {} timed out after {}ms", stepId,
                step.getTimeout().toMillis());

            StepResult<O> result = StepResult.timedOut(startTime, endTime, attemptNumber);
            eventPort.publishStepFailed(instanceId, stepId, result);

            return result;

        } catch (Exception e) {
            Instant endTime = Instant.now();

            // Execute error hook
            step.onError(e, context);

            log.error("Step {} failed with exception: {}", stepId, e.getMessage(), e);

            StepResult<O> result = StepResult.failure(e, startTime, endTime, attemptNumber);
            eventPort.publishStepFailed(instanceId, stepId, result);

            throw new RuntimeException(e); // Re-throw for retry handler
        }
    }

    /**
     * Execute compensation for a step
     *
     * @param compensationAction The compensation action
     * @param input The input (typically step output)
     * @param context The workflow context
     * @param stepId The step ID
     * @param instanceId The workflow instance ID
     * @param <I> Input type
     * @return true if compensation succeeded
     */
    public <I> boolean executeCompensation(
        CompensationAction<I> compensationAction,
        I input,
        WorkflowContext context,
        String stepId,
        String instanceId
    ) {
        log.info("Executing compensation for step: {}", stepId);

        try {
            compensationAction.compensate(input, context);
            log.info("Compensation succeeded for step: {}", stepId);
            eventPort.publishStepCompensated(instanceId, stepId);
            return true;

        } catch (Exception e) {
            log.error("Compensation failed for step: {}", stepId, e);
            return false;
        }
    }
}
