package fr.postiqa.core.domain.workflow.port;

import fr.postiqa.core.domain.workflow.model.StepResult;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;

/**
 * Port for publishing workflow events.
 * Enables observability and monitoring of workflow execution.
 * This is a secondary port (driven/out) - implemented by infrastructure layer.
 */
public interface WorkflowEventPort {

    /**
     * Publish event when a workflow starts
     *
     * @param instance The workflow instance that started
     */
    void publishWorkflowStarted(WorkflowInstance instance);

    /**
     * Publish event when a workflow completes successfully
     *
     * @param instance The workflow instance that completed
     */
    void publishWorkflowCompleted(WorkflowInstance instance);

    /**
     * Publish event when a workflow fails
     *
     * @param instance The workflow instance that failed
     * @param reason The failure reason
     */
    void publishWorkflowFailed(WorkflowInstance instance, String reason);

    /**
     * Publish event when a workflow is paused
     *
     * @param instance The workflow instance that was paused
     */
    void publishWorkflowPaused(WorkflowInstance instance);

    /**
     * Publish event when a workflow is resumed
     *
     * @param instance The workflow instance that was resumed
     */
    void publishWorkflowResumed(WorkflowInstance instance);

    /**
     * Publish event when a workflow is cancelled
     *
     * @param instance The workflow instance that was cancelled
     */
    void publishWorkflowCancelled(WorkflowInstance instance);

    /**
     * Publish event when compensation starts
     *
     * @param instance The workflow instance being compensated
     */
    void publishCompensationStarted(WorkflowInstance instance);

    /**
     * Publish event when compensation completes
     *
     * @param instance The workflow instance that was compensated
     */
    void publishCompensationCompleted(WorkflowInstance instance);

    /**
     * Publish event when a step starts
     *
     * @param instanceId The workflow instance ID
     * @param stepId The step ID
     */
    void publishStepStarted(String instanceId, String stepId);

    /**
     * Publish event when a step completes
     *
     * @param instanceId The workflow instance ID
     * @param stepId The step ID
     * @param result The step result
     */
    void publishStepCompleted(String instanceId, String stepId, StepResult<?> result);

    /**
     * Publish event when a step fails
     *
     * @param instanceId The workflow instance ID
     * @param stepId The step ID
     * @param result The step result containing the error
     */
    void publishStepFailed(String instanceId, String stepId, StepResult<?> result);

    /**
     * Publish event when a step is retried
     *
     * @param instanceId The workflow instance ID
     * @param stepId The step ID
     * @param attemptNumber The retry attempt number
     */
    void publishStepRetried(String instanceId, String stepId, int attemptNumber);

    /**
     * Publish event when a step is skipped
     *
     * @param instanceId The workflow instance ID
     * @param stepId The step ID
     */
    void publishStepSkipped(String instanceId, String stepId);

    /**
     * Publish event when a step is compensated
     *
     * @param instanceId The workflow instance ID
     * @param stepId The step ID
     */
    void publishStepCompensated(String instanceId, String stepId);
}
