package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.model.StepResult;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.workflow.engine.StepExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Use case for executing an individual workflow step.
 * Useful for testing steps in isolation or custom step execution logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecuteWorkflowStepUseCase {

    private final StepExecutor stepExecutor;

    /**
     * Execute a single step synchronously
     *
     * @param step The step to execute
     * @param input The input data for the step
     * @param context The workflow context
     * @param instanceId The workflow instance ID (for event tracking)
     * @param <I> Input type
     * @param <O> Output type
     * @return The step result
     */
    public <I, O> StepResult<O> execute(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context,
        String instanceId
    ) {
        log.info("Executing individual step: {} for instance: {}", step.getStepId(), instanceId);
        return stepExecutor.executeStep(step, input, context, instanceId);
    }

    /**
     * Execute a single step with null input
     */
    public <O> StepResult<O> execute(
        WorkflowStep<Void, O> step,
        WorkflowContext context,
        String instanceId
    ) {
        return execute(step, null, context, instanceId);
    }

    /**
     * Execute a single step with empty context
     */
    public <I, O> StepResult<O> execute(
        WorkflowStep<I, O> step,
        I input,
        String instanceId
    ) {
        return execute(step, input, WorkflowContext.empty(), instanceId);
    }

    /**
     * Execute a single step asynchronously
     *
     * @param step The step to execute
     * @param input The input data for the step
     * @param context The workflow context
     * @param instanceId The workflow instance ID
     * @param <I> Input type
     * @param <O> Output type
     * @return CompletableFuture of the step result
     */
    @Async("workflowExecutor")
    public <I, O> CompletableFuture<StepResult<O>> executeAsync(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context,
        String instanceId
    ) {
        log.info("Executing step asynchronously: {} for instance: {}",
            step.getStepId(), instanceId);
        return stepExecutor.executeStepAsync(step, input, context, instanceId);
    }

    /**
     * Test a step execution (for development/testing)
     * Uses a dummy instance ID
     */
    public <I, O> StepResult<O> testStep(
        WorkflowStep<I, O> step,
        I input,
        WorkflowContext context
    ) {
        String testInstanceId = "test-" + System.currentTimeMillis();
        log.info("Testing step: {} with instance ID: {}", step.getStepId(), testInstanceId);
        return execute(step, input, context, testInstanceId);
    }

    /**
     * Test a step execution with empty context
     */
    public <I, O> StepResult<O> testStep(WorkflowStep<I, O> step, I input) {
        return testStep(step, input, WorkflowContext.empty());
    }

    /**
     * Test a step execution with null input and empty context
     */
    public <O> StepResult<O> testStep(WorkflowStep<Void, O> step) {
        return testStep(step, null, WorkflowContext.empty());
    }
}
