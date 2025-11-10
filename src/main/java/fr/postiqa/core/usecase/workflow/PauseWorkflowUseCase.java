package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for pausing a running workflow.
 * Workflow can be resumed later from the same state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PauseWorkflowUseCase {

    private final WorkflowPersistencePort persistencePort;
    private final WorkflowEventPort eventPort;

    /**
     * Pause a running workflow
     *
     * @param instanceId The workflow instance ID
     * @return The updated workflow instance
     * @throws IllegalStateException if workflow cannot be paused
     */
    public WorkflowInstance execute(String instanceId) {
        log.info("Pausing workflow: {}", instanceId);

        // Retrieve workflow instance
        WorkflowInstance instance = persistencePort.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Workflow instance not found: %s", instanceId)
            ));

        // Check if workflow can be paused
        if (!instance.getStatus().canPause()) {
            throw new IllegalStateException(
                String.format("Cannot pause workflow in status: %s", instance.getStatus())
            );
        }

        // Pause the workflow
        instance.pause();

        // Persist the updated state
        persistencePort.save(instance);

        // Publish event
        eventPort.publishWorkflowPaused(instance);

        log.info("Workflow {} paused successfully", instanceId);

        return instance;
    }

    /**
     * Pause a workflow if it's running
     *
     * @param instanceId The workflow instance ID
     * @return true if paused, false if already paused or not pausable
     */
    public boolean pauseIfRunning(String instanceId) {
        try {
            execute(instanceId);
            return true;
        } catch (IllegalStateException e) {
            log.debug("Workflow {} cannot be paused: {}", instanceId, e.getMessage());
            return false;
        }
    }
}
