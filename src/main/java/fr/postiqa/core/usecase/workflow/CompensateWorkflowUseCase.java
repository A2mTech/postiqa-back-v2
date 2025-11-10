package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.enums.CompensationStrategy;
import fr.postiqa.core.domain.workflow.model.CompensationAction;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import fr.postiqa.core.infrastructure.workflow.resilience.CompensationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Use case for manually triggering compensation (rollback) on a workflow.
 * This is typically used when automatic compensation fails or for manual intervention.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompensateWorkflowUseCase {

    private final CompensationHandler compensationHandler;
    private final WorkflowPersistencePort persistencePort;
    private final WorkflowEventPort eventPort;

    /**
     * Execute compensation for a failed workflow
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition (needed for compensation actions)
     * @return The compensated workflow instance
     * @throws IllegalStateException if workflow cannot be compensated
     */
    public WorkflowInstance execute(String instanceId, WorkflowDefinition definition) {
        log.info("Starting manual compensation for workflow: {}", instanceId);

        // Retrieve workflow instance
        WorkflowInstance instance = persistencePort.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Workflow instance not found: %s", instanceId)
            ));

        // Check if workflow can be compensated
        if (!instance.getStatus().canCompensate()) {
            throw new IllegalStateException(
                String.format("Cannot compensate workflow in status: %s", instance.getStatus())
            );
        }

        // Check if compensation is enabled in definition
        if (!definition.hasCompensation()) {
            log.warn("Compensation strategy is NONE for workflow: {}", definition.name());
            throw new IllegalStateException(
                "Compensation is not enabled for this workflow"
            );
        }

        // Start compensation
        instance.startCompensation();
        persistencePort.save(instance);
        eventPort.publishCompensationStarted(instance);

        // Collect completed steps with outputs
        List<CompensationHandler.CompensatedStep> completedSteps = collectCompletedSteps(instance);
        Map<String, CompensationAction<?>> compensationActions = collectCompensationActions(
            definition,
            instance.getCompletedStepIds()
        );

        log.info("Compensating {} completed steps", completedSteps.size());

        // Execute compensation
        boolean success = compensationHandler.executeCompensation(
            completedSteps,
            compensationActions,
            definition.compensationStrategy(),
            instance.getContext()
        );

        // Update workflow state
        if (success) {
            instance.completeCompensation();
            eventPort.publishCompensationCompleted(instance);
            log.info("Compensation completed successfully for workflow: {}", instanceId);
        } else {
            log.error("Compensation partially failed for workflow: {}", instanceId);
        }

        // Persist final state
        persistencePort.save(instance);

        return instance;
    }

    /**
     * Compensate a workflow if it's in failed state
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition
     * @return true if compensated, false if not in compensable state
     */
    public boolean compensateIfFailed(String instanceId, WorkflowDefinition definition) {
        try {
            execute(instanceId, definition);
            return true;
        } catch (IllegalStateException e) {
            log.debug("Workflow {} cannot be compensated: {}", instanceId, e.getMessage());
            return false;
        }
    }

    /**
     * Collect completed steps with their outputs
     */
    private List<CompensationHandler.CompensatedStep> collectCompletedSteps(WorkflowInstance instance) {
        List<CompensationHandler.CompensatedStep> steps = new ArrayList<>();

        for (String stepId : instance.getCompletedStepIds()) {
            instance.getStepExecution(stepId).ifPresent(execution -> {
                execution.output().ifPresent(output -> {
                    steps.add(CompensationHandler.CompensatedStep.of(stepId, output));
                });
            });
        }

        return steps;
    }

    /**
     * Collect compensation actions from workflow definition
     */
    @SuppressWarnings("unchecked")
    private Map<String, CompensationAction<?>> collectCompensationActions(
        WorkflowDefinition definition,
        Set<String> completedStepIds
    ) {
        Map<String, CompensationAction<?>> actions = new HashMap<>();

        for (String stepId : completedStepIds) {
            definition.getStep(stepId).ifPresent(step -> {
                step.getCompensationAction().ifPresent(action -> {
                    actions.put(stepId, (CompensationAction<?>) action);
                });
            });
        }

        return actions;
    }
}
