package fr.postiqa.core.adapter.in.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.infrastructure.workflow.monitoring.WorkflowMetrics;
import fr.postiqa.core.infrastructure.workflow.state.StepStateTracker;
import fr.postiqa.core.infrastructure.workflow.state.WorkflowStateManager;
import fr.postiqa.core.usecase.workflow.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of WorkflowOrchestrator.
 * Delegates to use cases and provides state/monitoring access.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowOrchestratorImpl implements WorkflowOrchestrator {

    // Use cases
    private final StartWorkflowUseCase startWorkflowUseCase;
    private final GetWorkflowStatusUseCase getWorkflowStatusUseCase;
    private final PauseWorkflowUseCase pauseWorkflowUseCase;
    private final ResumeWorkflowUseCase resumeWorkflowUseCase;
    private final CancelWorkflowUseCase cancelWorkflowUseCase;
    private final CompensateWorkflowUseCase compensateWorkflowUseCase;

    // State and monitoring
    private final WorkflowStateManager stateManager;
    private final StepStateTracker stepStateTracker;
    private final WorkflowMetrics metrics;

    // ==================== Workflow Execution ====================

    @Override
    public WorkflowInstance startWorkflow(WorkflowDefinition definition, WorkflowContext initialContext) {
        log.info("Starting workflow via orchestrator: {}", definition.name());
        return startWorkflowUseCase.execute(definition, initialContext);
    }

    @Override
    public WorkflowInstance startWorkflow(WorkflowDefinition definition) {
        return startWorkflowUseCase.execute(definition);
    }

    @Override
    public CompletableFuture<WorkflowInstance> startWorkflowAsync(
        WorkflowDefinition definition,
        WorkflowContext initialContext
    ) {
        log.info("Starting workflow asynchronously via orchestrator: {}", definition.name());
        return startWorkflowUseCase.executeAsync(definition, initialContext);
    }

    @Override
    public CompletableFuture<WorkflowInstance> startWorkflowAsync(WorkflowDefinition definition) {
        return startWorkflowUseCase.executeAsync(definition);
    }

    @Override
    public String startWorkflowAndGetId(WorkflowDefinition definition, WorkflowContext initialContext) {
        log.info("Starting workflow and returning ID: {}", definition.name());
        return startWorkflowUseCase.startAndReturnId(definition, initialContext);
    }

    // ==================== Workflow Control ====================

    @Override
    public WorkflowInstance pauseWorkflow(String instanceId) {
        log.info("Pausing workflow via orchestrator: {}", instanceId);
        return pauseWorkflowUseCase.execute(instanceId);
    }

    @Override
    public WorkflowInstance resumeWorkflow(String instanceId, WorkflowDefinition definition) {
        log.info("Resuming workflow via orchestrator: {}", instanceId);
        return resumeWorkflowUseCase.execute(instanceId, definition);
    }

    @Override
    public CompletableFuture<WorkflowInstance> resumeWorkflowAsync(
        String instanceId,
        WorkflowDefinition definition
    ) {
        log.info("Resuming workflow asynchronously via orchestrator: {}", instanceId);
        return resumeWorkflowUseCase.executeAsync(instanceId, definition);
    }

    @Override
    public WorkflowInstance cancelWorkflow(String instanceId) {
        log.info("Cancelling workflow via orchestrator: {}", instanceId);
        return cancelWorkflowUseCase.execute(instanceId);
    }

    @Override
    public WorkflowInstance compensateWorkflow(String instanceId, WorkflowDefinition definition) {
        log.info("Compensating workflow via orchestrator: {}", instanceId);
        return compensateWorkflowUseCase.execute(instanceId, definition);
    }

    // ==================== Workflow Status ====================

    @Override
    public Optional<WorkflowInstance> getWorkflow(String instanceId) {
        return getWorkflowStatusUseCase.execute(instanceId);
    }

    @Override
    public WorkflowInstance getWorkflowOrThrow(String instanceId) {
        return getWorkflowStatusUseCase.executeOrThrow(instanceId);
    }

    @Override
    public boolean workflowExists(String instanceId) {
        return getWorkflowStatusUseCase.exists(instanceId);
    }

    @Override
    public double getWorkflowProgress(String instanceId, int totalSteps) {
        return getWorkflowStatusUseCase.getProgress(instanceId, totalSteps);
    }

    // ==================== State Management ====================

    @Override
    public WorkflowStateManager.WorkflowStateSummary getStateSummary(String instanceId) {
        WorkflowInstance instance = getWorkflowOrThrow(instanceId);
        return stateManager.getStateSummary(instance);
    }

    @Override
    public StepStateTracker.WorkflowExecutionStats getExecutionStats(String instanceId) {
        WorkflowInstance instance = getWorkflowOrThrow(instanceId);
        return stepStateTracker.getExecutionStats(instance);
    }

    @Override
    public WorkflowStateManager.HealthStatus checkHealth(String instanceId) {
        WorkflowInstance instance = getWorkflowOrThrow(instanceId);
        // Use 1 hour as default threshold for stuck detection
        return stateManager.checkHealth(instance, Duration.ofHours(1));
    }

    // ==================== Monitoring ====================

    @Override
    public WorkflowMetrics.MetricsSnapshot getMetricsSnapshot() {
        return metrics.getSnapshot();
    }
}
