package fr.postiqa.core.infrastructure.workflow.engine;

import fr.postiqa.core.domain.workflow.model.StepResult;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Coordinates parallel execution of workflow steps.
 * Manages concurrent execution and aggregates results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParallelExecutionCoordinator {

    private final StepExecutor stepExecutor;

    /**
     * Execute multiple steps in parallel
     *
     * @param steps Map of step ID to step instance
     * @param inputs Map of step ID to input data
     * @param context Shared workflow context
     * @param instanceId Workflow instance ID
     * @return Map of step ID to step result
     */
    public Map<String, StepResult<?>> executeInParallel(
        Map<String, WorkflowStep<?, ?>> steps,
        Map<String, Object> inputs,
        WorkflowContext context,
        String instanceId
    ) {
        log.info("Executing {} steps in parallel for workflow instance: {}",
            steps.size(), instanceId);

        if (steps.isEmpty()) {
            return Collections.emptyMap();
        }

        // Create futures for all steps
        Map<String, CompletableFuture<StepResult<?>>> futures = new LinkedHashMap<>();

        for (Map.Entry<String, WorkflowStep<?, ?>> entry : steps.entrySet()) {
            String stepId = entry.getKey();
            WorkflowStep step = entry.getValue();
            Object input = inputs.get(stepId);

            CompletableFuture<StepResult<?>> future = executeStepAsync(
                step,
                input,
                context,
                instanceId
            );

            futures.put(stepId, future);
        }

        // Wait for all to complete and collect results
        return collectResults(futures);
    }

    /**
     * Execute a set of step IDs in parallel using their definitions
     *
     * @param stepIds Set of step IDs to execute
     * @param stepDefinitions Map of all step definitions
     * @param inputs Map of inputs
     * @param context Workflow context
     * @param instanceId Workflow instance ID
     * @return Map of step results
     */
    public Map<String, StepResult<?>> executeStepsByIds(
        Set<String> stepIds,
        Map<String, WorkflowStep<?, ?>> stepDefinitions,
        Map<String, Object> inputs,
        WorkflowContext context,
        String instanceId
    ) {
        Map<String, WorkflowStep<?, ?>> stepsToExecute = new LinkedHashMap<>();

        for (String stepId : stepIds) {
            WorkflowStep<?, ?> step = stepDefinitions.get(stepId);
            if (step == null) {
                log.error("Step definition not found for ID: {}", stepId);
                throw new IllegalArgumentException("Step not found: " + stepId);
            }
            stepsToExecute.put(stepId, step);
        }

        return executeInParallel(stepsToExecute, inputs, context, instanceId);
    }

    /**
     * Execute a single step asynchronously (wrapper with type safety)
     */
    @SuppressWarnings("unchecked")
    private <I, O> CompletableFuture<StepResult<?>> executeStepAsync(
        WorkflowStep<I, O> step,
        Object input,
        WorkflowContext context,
        String instanceId
    ) {
        return (CompletableFuture<StepResult<?>>) (CompletableFuture<?>)
            stepExecutor.executeStepAsync(step, (I) input, context, instanceId);
    }

    /**
     * Collect results from all futures
     */
    private Map<String, StepResult<?>> collectResults(
        Map<String, CompletableFuture<StepResult<?>>> futures
    ) {
        Map<String, StepResult<?>> results = new LinkedHashMap<>();

        for (Map.Entry<String, CompletableFuture<StepResult<?>>> entry : futures.entrySet()) {
            String stepId = entry.getKey();
            CompletableFuture<StepResult<?>> future = entry.getValue();

            try {
                StepResult<?> result = future.get(); // Block until complete
                results.put(stepId, result);
                log.debug("Step {} completed with status: {}", stepId, result.status());

            } catch (ExecutionException e) {
                log.error("Step {} failed with exception", stepId, e.getCause());
                // Create failure result
                StepResult<?> failureResult = StepResult.failure(
                    (Exception) e.getCause(),
                    java.time.Instant.now(),
                    java.time.Instant.now(),
                    0
                );
                results.put(stepId, failureResult);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Step {} execution interrupted", stepId);
                throw new RuntimeException("Parallel execution interrupted", e);
            }
        }

        long successCount = results.values().stream()
            .filter(StepResult::isSuccess)
            .count();

        log.info("Parallel execution complete. Success: {}/{}", successCount, results.size());

        return results;
    }

    /**
     * Check if all results are successful
     */
    public boolean allSuccessful(Map<String, StepResult<?>> results) {
        return results.values().stream().allMatch(StepResult::isSuccess);
    }

    /**
     * Get failed step IDs from results
     */
    public Set<String> getFailedStepIds(Map<String, StepResult<?>> results) {
        Set<String> failed = new HashSet<>();
        for (Map.Entry<String, StepResult<?>> entry : results.entrySet()) {
            if (entry.getValue().isFailure()) {
                failed.add(entry.getKey());
            }
        }
        return failed;
    }
}
