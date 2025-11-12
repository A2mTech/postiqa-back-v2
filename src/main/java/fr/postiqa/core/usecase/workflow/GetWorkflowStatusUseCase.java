package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving workflow status and state.
 * Provides read-only access to workflow instances.
 */
@fr.postiqa.shared.annotation.UseCase(
    value = "GetWorkflowStatus",
    resourceType = "WORKFLOW",
    description = "Retrieves workflow execution status and state",
    logActivity = false  // Read-only query operation
)
@Service
@RequiredArgsConstructor
@Slf4j
public class GetWorkflowStatusUseCase {

    private final WorkflowPersistencePort persistencePort;

    /**
     * Get workflow instance by ID
     *
     * @param instanceId The workflow instance ID
     * @return Optional containing the workflow instance if found
     */
    public Optional<WorkflowInstance> execute(String instanceId) {
        log.debug("Retrieving workflow status for instance: {}", instanceId);
        return persistencePort.findById(instanceId);
    }

    /**
     * Get workflow instance or throw exception
     *
     * @param instanceId The workflow instance ID
     * @return The workflow instance
     * @throws WorkflowNotFoundException if workflow not found
     */
    public WorkflowInstance executeOrThrow(String instanceId) {
        return execute(instanceId)
            .orElseThrow(() -> new WorkflowNotFoundException(
                String.format("Workflow instance not found: %s", instanceId)
            ));
    }

    /**
     * Check if workflow exists
     *
     * @param instanceId The workflow instance ID
     * @return true if workflow exists
     */
    public boolean exists(String instanceId) {
        return persistencePort.existsById(instanceId);
    }

    /**
     * Get workflow progress (0.0 to 1.0)
     *
     * @param instanceId The workflow instance ID
     * @param totalSteps Total number of steps in the workflow
     * @return Progress percentage
     */
    public double getProgress(String instanceId, int totalSteps) {
        return execute(instanceId)
            .map(instance -> instance.getProgress(totalSteps))
            .orElse(0.0);
    }

    /**
     * Exception thrown when workflow not found
     */
    public static class WorkflowNotFoundException extends RuntimeException {
        public WorkflowNotFoundException(String message) {
            super(message);
        }
    }
}
