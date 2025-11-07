package fr.postiqa.core.domain.workflow.port;

import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and retrieving workflow instances.
 * This is a secondary port (driven/out) - implemented by infrastructure layer.
 */
public interface WorkflowPersistencePort {

    /**
     * Save a workflow instance (create or update)
     *
     * @param instance The workflow instance to save
     * @return The saved instance with any generated fields
     */
    WorkflowInstance save(WorkflowInstance instance);

    /**
     * Find a workflow instance by ID
     *
     * @param instanceId The instance ID
     * @return Optional containing the instance if found
     */
    Optional<WorkflowInstance> findById(String instanceId);

    /**
     * Find all workflow instances for a given workflow name
     *
     * @param workflowName The workflow name
     * @return List of instances
     */
    List<WorkflowInstance> findByWorkflowName(String workflowName);

    /**
     * Find all workflow instances with a specific status
     *
     * @param status The workflow status
     * @return List of instances
     */
    List<WorkflowInstance> findByStatus(WorkflowStatus status);

    /**
     * Find all workflow instances with a specific status for a workflow name
     *
     * @param workflowName The workflow name
     * @param status The workflow status
     * @return List of instances
     */
    List<WorkflowInstance> findByWorkflowNameAndStatus(String workflowName, WorkflowStatus status);

    /**
     * Delete a workflow instance by ID
     *
     * @param instanceId The instance ID
     * @return true if deleted, false if not found
     */
    boolean deleteById(String instanceId);

    /**
     * Check if a workflow instance exists
     *
     * @param instanceId The instance ID
     * @return true if exists, false otherwise
     */
    boolean existsById(String instanceId);

    /**
     * Count total workflow instances
     *
     * @return The total count
     */
    long count();

    /**
     * Count workflow instances by status
     *
     * @param status The workflow status
     * @return The count
     */
    long countByStatus(WorkflowStatus status);
}
