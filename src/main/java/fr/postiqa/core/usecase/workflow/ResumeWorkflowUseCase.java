package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import fr.postiqa.core.infrastructure.workflow.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Use case for resuming a paused workflow.
 * Continues execution from the last completed step.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeWorkflowUseCase {

    private final WorkflowEngine workflowEngine;
    private final WorkflowPersistencePort persistencePort;
    private final WorkflowEventPort eventPort;

    /**
     * Resume a paused workflow synchronously
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition (needed to continue execution)
     * @return The updated workflow instance after resumption
     * @throws IllegalStateException if workflow cannot be resumed
     */
    public WorkflowInstance execute(String instanceId, WorkflowDefinition definition) {
        log.info("Resuming workflow: {}", instanceId);

        // Retrieve workflow instance
        WorkflowInstance instance = persistencePort.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Workflow instance not found: %s", instanceId)
            ));

        // Check if workflow can be resumed
        if (!instance.getStatus().canResume()) {
            throw new IllegalStateException(
                String.format("Cannot resume workflow in status: %s", instance.getStatus())
            );
        }

        // Verify workflow definition matches
        if (!instance.getWorkflowName().equals(definition.name())) {
            throw new IllegalArgumentException(
                String.format("Workflow definition mismatch. Instance: %s, Definition: %s",
                    instance.getWorkflowName(), definition.name())
            );
        }

        // Resume the workflow
        instance.resume();

        // Persist the updated state
        persistencePort.save(instance);

        // Publish event
        eventPort.publishWorkflowResumed(instance);

        log.info("Workflow {} resumed, continuing execution", instanceId);

        // Continue execution from where it left off
        return workflowEngine.executeWorkflow(definition, instance);
    }

    /**
     * Resume a paused workflow asynchronously
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition
     * @return CompletableFuture of the workflow instance
     */
    @Async("workflowExecutor")
    public CompletableFuture<WorkflowInstance> executeAsync(
        String instanceId,
        WorkflowDefinition definition
    ) {
        log.info("Resuming workflow asynchronously: {}", instanceId);
        return CompletableFuture.supplyAsync(() -> execute(instanceId, definition));
    }

    /**
     * Resume a workflow if it's paused
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition
     * @return true if resumed, false if not pausable or already running
     */
    public boolean resumeIfPaused(String instanceId, WorkflowDefinition definition) {
        try {
            execute(instanceId, definition);
            return true;
        } catch (IllegalStateException e) {
            log.debug("Workflow {} cannot be resumed: {}", instanceId, e.getMessage());
            return false;
        }
    }
}
