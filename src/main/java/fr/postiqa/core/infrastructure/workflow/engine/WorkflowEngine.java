package fr.postiqa.core.infrastructure.workflow.engine;

import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import fr.postiqa.core.domain.workflow.model.*;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import fr.postiqa.core.infrastructure.workflow.resilience.CompensationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Central workflow execution engine.
 * Orchestrates workflow execution using dependency resolution, parallel coordination,
 * and step execution with full resilience features.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final DependencyResolver dependencyResolver;
    private final StepExecutor stepExecutor;
    private final ParallelExecutionCoordinator parallelCoordinator;
    private final CompensationHandler compensationHandler;
    private final WorkflowPersistencePort persistencePort;
    private final WorkflowEventPort eventPort;

    /**
     * Execute a complete workflow from start to finish
     *
     * @param definition The workflow definition
     * @param instance The workflow instance (must be in PENDING or RUNNING state)
     * @return The updated workflow instance with final state
     */
    public WorkflowInstance executeWorkflow(
        WorkflowDefinition definition,
        WorkflowInstance instance
    ) {
        String instanceId = instance.getInstanceId();
        log.info("Starting workflow execution: {} (instance: {})",
            definition.name(), instanceId);

        try {
            // Start workflow if pending
            if (instance.getStatus() == WorkflowStatus.PENDING) {
                instance.start();
                persistencePort.save(instance);
                eventPort.publishWorkflowStarted(instance);
            }

            // Resolve execution layers (DAG topological sort)
            List<Set<String>> executionLayers = dependencyResolver.resolveExecutionOrder(definition);

            log.info("Workflow {} has {} execution layers",
                definition.name(), executionLayers.size());

            // Execute each layer
            for (int layerIndex = 0; layerIndex < executionLayers.size(); layerIndex++) {
                Set<String> currentLayer = executionLayers.get(layerIndex);

                log.info("Executing layer {}/{} with {} steps",
                    layerIndex + 1, executionLayers.size(), currentLayer.size());

                boolean layerSuccess = executeLayer(
                    currentLayer,
                    definition,
                    instance
                );

                if (!layerSuccess) {
                    log.error("Layer {} failed, stopping workflow execution", layerIndex + 1);
                    handleWorkflowFailure(definition, instance);
                    return instance;
                }

                // Save progress after each layer
                persistencePort.save(instance);
            }

            // All layers completed successfully
            instance.complete();
            persistencePort.save(instance);
            eventPort.publishWorkflowCompleted(instance);

            log.info("Workflow {} completed successfully", definition.name());

            return instance;

        } catch (Exception e) {
            log.error("Unexpected error during workflow execution", e);
            handleWorkflowFailure(definition, instance, e.getMessage());
            return instance;
        }
    }

    /**
     * Execute a single layer of steps (sequential or parallel based on definition)
     */
    @SuppressWarnings("unchecked")
    private boolean executeLayer(
        Set<String> stepIds,
        WorkflowDefinition definition,
        WorkflowInstance instance
    ) {
        Map<String, StepResult<?>> results;

        if (definition.isParallel() && stepIds.size() > 1) {
            // Execute in parallel
            log.debug("Executing {} steps in parallel", stepIds.size());
            results = executeStepsInParallel(stepIds, definition, instance);
        } else {
            // Execute sequentially
            log.debug("Executing {} steps sequentially", stepIds.size());
            results = executeStepsSequentially(stepIds, definition, instance);
        }

        // Record all step executions
        for (Map.Entry<String, StepResult<?>> entry : results.entrySet()) {
            String stepId = entry.getKey();
            StepResult<?> result = entry.getValue();

            WorkflowInstance.StepExecution execution =
                WorkflowInstance.StepExecution.fromResult(stepId, result);

            instance.recordStepExecution(stepId, execution);

            // Store output in context if configured
            if (result.isSuccess() && result.output().isPresent()) {
                WorkflowStep<?, ?> step = definition.getStep(stepId).orElseThrow();
                step.getOutputKey().ifPresent(outputKey -> {
                    WorkflowContext updatedContext = instance.getContext()
                        .put(outputKey, result.output().get());
                    instance.updateContext(updatedContext);
                });
            }
        }

        // Check if all successful
        return results.values().stream().allMatch(StepResult::isSuccess);
    }

    /**
     * Execute steps in parallel
     */
    private Map<String, StepResult<?>> executeStepsInParallel(
        Set<String> stepIds,
        WorkflowDefinition definition,
        WorkflowInstance instance
    ) {
        Map<String, Object> inputs = prepareInputs(stepIds, definition, instance.getContext());

        return parallelCoordinator.executeStepsByIds(
            stepIds,
            definition.steps(),
            inputs,
            instance.getContext(),
            instance.getInstanceId()
        );
    }

    /**
     * Execute steps sequentially
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, StepResult<?>> executeStepsSequentially(
        Set<String> stepIds,
        WorkflowDefinition definition,
        WorkflowInstance instance
    ) {
        Map<String, StepResult<?>> results = new LinkedHashMap<>();

        for (String stepId : stepIds) {
            WorkflowStep step = definition.getStep(stepId)
                .orElseThrow(() -> new IllegalStateException("Step not found: " + stepId));

            Object input = prepareInput(stepId, step, instance.getContext());

            StepResult result = stepExecutor.executeStep(
                step,
                input,
                instance.getContext(),
                instance.getInstanceId()
            );

            results.put(stepId, result);

            // Stop on first failure in sequential mode
            if (result.isFailure()) {
                log.error("Step {} failed, stopping sequential execution", stepId);
                break;
            }

            // Update context with output
            if (result.isSuccess() && result.output().isPresent()) {
                step.getOutputKey().ifPresent(outputKey -> {
                    WorkflowContext updatedContext = instance.getContext()
                        .put((String) outputKey, result.output().get());
                    instance.updateContext(updatedContext);
                });
            }
        }

        return results;
    }

    /**
     * Prepare inputs for all steps
     */
    private Map<String, Object> prepareInputs(
        Set<String> stepIds,
        WorkflowDefinition definition,
        WorkflowContext context
    ) {
        Map<String, Object> inputs = new HashMap<>();

        for (String stepId : stepIds) {
            WorkflowStep<?, ?> step = definition.getStep(stepId).orElseThrow();
            Object input = prepareInput(stepId, step, context);
            inputs.put(stepId, input);
        }

        return inputs;
    }

    /**
     * Prepare input for a single step
     */
    @SuppressWarnings("unchecked")
    private <I> I prepareInput(String stepId, WorkflowStep<I, ?> step, WorkflowContext context) {
        // If step specifies an input key, get from context
        Optional<String> inputKey = step.getInputKey();
        if (inputKey.isPresent()) {
            return (I) context.getRequired(inputKey.get(), Object.class);
        }

        // Otherwise, return null (step will handle it)
        return null;
    }

    /**
     * Handle workflow failure with compensation
     */
    private void handleWorkflowFailure(WorkflowDefinition definition, WorkflowInstance instance) {
        handleWorkflowFailure(definition, instance, "One or more steps failed");
    }

    /**
     * Handle workflow failure with compensation
     */
    private void handleWorkflowFailure(
        WorkflowDefinition definition,
        WorkflowInstance instance,
        String reason
    ) {
        log.error("Workflow {} failed: {}", definition.name(), reason);

        instance.fail(reason);
        persistencePort.save(instance);
        eventPort.publishWorkflowFailed(instance, reason);

        // Execute compensation if enabled
        if (definition.hasCompensation()) {
            executeCompensation(definition, instance);
        }
    }

    /**
     * Execute compensation for completed steps
     */
    private void executeCompensation(WorkflowDefinition definition, WorkflowInstance instance) {
        log.info("Starting compensation for workflow: {}", definition.name());

        instance.startCompensation();
        persistencePort.save(instance);
        eventPort.publishCompensationStarted(instance);

        // Collect completed steps with outputs
        List<CompensationHandler.CompensatedStep> completedSteps = new ArrayList<>();
        Map<String, CompensationAction<?>> compensationActions = new HashMap<>();

        for (String stepId : instance.getCompletedStepIds()) {
            Optional<WorkflowInstance.StepExecution> execution = instance.getStepExecution(stepId);
            execution.ifPresent(exec -> {
                exec.output().ifPresent(output -> {
                    completedSteps.add(CompensationHandler.CompensatedStep.of(stepId, output));
                });
            });

            // Get compensation action from step definition
            definition.getStep(stepId).ifPresent(step -> {
                step.getCompensationAction().ifPresent(action -> {
                    compensationActions.put(stepId, action);
                });
            });
        }

        // Execute compensation
        boolean success = compensationHandler.executeCompensation(
            completedSteps,
            compensationActions,
            definition.compensationStrategy(),
            instance.getContext()
        );

        if (success) {
            instance.completeCompensation();
            eventPort.publishCompensationCompleted(instance);
            log.info("Compensation completed successfully");
        } else {
            log.error("Compensation partially failed");
        }

        persistencePort.save(instance);
    }
}
