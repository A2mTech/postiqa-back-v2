package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for cancelling a workflow execution.
 * Cancelled workflows cannot be resumed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancelWorkflowUseCase {

    private final WorkflowPersistencePort persistencePort;
    private final WorkflowEventPort eventPort;

    /**
     * Cancel a running or paused workflow
     *
     * @param instanceId The workflow instance ID
     * @return The cancelled workflow instance
     * @throws IllegalStateException if workflow is already in terminal state
     */
    public WorkflowInstance execute(String instanceId) {
        log.info("Cancelling workflow: {}", instanceId);

        // Retrieve workflow instance
        WorkflowInstance instance = persistencePort.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Workflow instance not found: %s", instanceId)
            ));

        // Check if workflow is already in terminal state
        if (instance.getStatus().isTerminal()) {
            throw new IllegalStateException(
                String.format("Cannot cancel workflow in terminal status: %s", instance.getStatus())
            );
        }

        // Cancel the workflow
        instance.cancel();

        // Persist the updated state
        persistencePort.save(instance);

        // Publish event
        eventPort.publishWorkflowCancelled(instance);

        log.info("Workflow {} cancelled successfully", instanceId);

        return instance;
    }

    /**
     * Cancel a workflow if it's not in terminal state
     *
     * @param instanceId The workflow instance ID
     * @return true if cancelled, false if already in terminal state
     */
    public boolean cancelIfNotTerminal(String instanceId) {
        try {
            execute(instanceId);
            return true;
        } catch (IllegalStateException e) {
            log.debug("Workflow {} cannot be cancelled: {}", instanceId, e.getMessage());
            return false;
        }
    }

    /**
     * Cancel multiple workflows
     *
     * @param instanceIds Array of workflow instance IDs
     * @return Number of workflows successfully cancelled
     */
    public int cancelAll(String... instanceIds) {
        int cancelledCount = 0;

        for (String instanceId : instanceIds) {
            if (cancelIfNotTerminal(instanceId)) {
                cancelledCount++;
            }
        }

        log.info("Cancelled {} out of {} workflows", cancelledCount, instanceIds.length);
        return cancelledCount;
    }
}
