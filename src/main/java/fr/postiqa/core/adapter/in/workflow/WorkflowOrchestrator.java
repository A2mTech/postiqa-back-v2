package fr.postiqa.core.adapter.in.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.infrastructure.workflow.monitoring.WorkflowMetrics;
import fr.postiqa.core.infrastructure.workflow.state.StepStateTracker;
import fr.postiqa.core.infrastructure.workflow.state.WorkflowStateManager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Primary adapter (IN) for workflow orchestration.
 * Public API for business and agency modules to interact with the workflow framework.
 *
 * This is the Facade pattern - hides complexity of use cases and provides
 * a simple, unified interface for workflow management.
 */
public interface WorkflowOrchestrator {

    // ==================== Workflow Execution ====================

    /**
     * Start a new workflow synchronously
     *
     * @param definition The workflow definition
     * @param initialContext Initial context data
     * @return The workflow instance after execution
     */
    WorkflowInstance startWorkflow(WorkflowDefinition definition, WorkflowContext initialContext);

    /**
     * Start a new workflow with empty context
     */
    WorkflowInstance startWorkflow(WorkflowDefinition definition);

    /**
     * Start a new workflow asynchronously
     *
     * @param definition The workflow definition
     * @param initialContext Initial context data
     * @return CompletableFuture of the workflow instance
     */
    CompletableFuture<WorkflowInstance> startWorkflowAsync(
        WorkflowDefinition definition,
        WorkflowContext initialContext
    );

    /**
     * Start a new workflow asynchronously with empty context
     */
    CompletableFuture<WorkflowInstance> startWorkflowAsync(WorkflowDefinition definition);

    /**
     * Start a workflow and return only the instance ID (fire-and-forget)
     *
     * @param definition The workflow definition
     * @param initialContext Initial context data
     * @return The instance ID for tracking
     */
    String startWorkflowAndGetId(WorkflowDefinition definition, WorkflowContext initialContext);

    // ==================== Workflow Control ====================

    /**
     * Pause a running workflow
     *
     * @param instanceId The workflow instance ID
     * @return The paused workflow instance
     */
    WorkflowInstance pauseWorkflow(String instanceId);

    /**
     * Resume a paused workflow
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition
     * @return The workflow instance after resumption
     */
    WorkflowInstance resumeWorkflow(String instanceId, WorkflowDefinition definition);

    /**
     * Resume a paused workflow asynchronously
     */
    CompletableFuture<WorkflowInstance> resumeWorkflowAsync(
        String instanceId,
        WorkflowDefinition definition
    );

    /**
     * Cancel a workflow execution
     *
     * @param instanceId The workflow instance ID
     * @return The cancelled workflow instance
     */
    WorkflowInstance cancelWorkflow(String instanceId);

    /**
     * Trigger manual compensation for a failed workflow
     *
     * @param instanceId The workflow instance ID
     * @param definition The workflow definition
     * @return The compensated workflow instance
     */
    WorkflowInstance compensateWorkflow(String instanceId, WorkflowDefinition definition);

    // ==================== Workflow Status ====================

    /**
     * Get workflow instance by ID
     *
     * @param instanceId The workflow instance ID
     * @return Optional containing the workflow instance if found
     */
    Optional<WorkflowInstance> getWorkflow(String instanceId);

    /**
     * Get workflow instance or throw exception
     *
     * @param instanceId The workflow instance ID
     * @return The workflow instance
     * @throws fr.postiqa.core.usecase.workflow.GetWorkflowStatusUseCase.WorkflowNotFoundException
     */
    WorkflowInstance getWorkflowOrThrow(String instanceId);

    /**
     * Check if workflow exists
     *
     * @param instanceId The workflow instance ID
     * @return true if workflow exists
     */
    boolean workflowExists(String instanceId);

    /**
     * Get workflow progress (0.0 to 1.0)
     *
     * @param instanceId The workflow instance ID
     * @param totalSteps Total number of steps in the workflow
     * @return Progress percentage
     */
    double getWorkflowProgress(String instanceId, int totalSteps);

    // ==================== State Management ====================

    /**
     * Get workflow state summary
     *
     * @param instanceId The workflow instance ID
     * @return State summary with detailed information
     */
    WorkflowStateManager.WorkflowStateSummary getStateSummary(String instanceId);

    /**
     * Get step execution statistics
     *
     * @param instanceId The workflow instance ID
     * @return Step execution statistics
     */
    StepStateTracker.WorkflowExecutionStats getExecutionStats(String instanceId);

    /**
     * Check workflow health
     *
     * @param instanceId The workflow instance ID
     * @return Health status
     */
    WorkflowStateManager.HealthStatus checkHealth(String instanceId);

    // ==================== Monitoring ====================

    /**
     * Get current metrics snapshot
     *
     * @return Metrics snapshot with all counters and timers
     */
    WorkflowMetrics.MetricsSnapshot getMetricsSnapshot();
}
