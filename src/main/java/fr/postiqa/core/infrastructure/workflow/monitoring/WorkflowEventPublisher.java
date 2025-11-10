package fr.postiqa.core.infrastructure.workflow.monitoring;

import fr.postiqa.core.adapter.out.workflow.SpringEventWorkflowEventAdapter.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to workflow events and publishes metrics.
 * Bridges workflow events with the monitoring system.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventPublisher {

    private final WorkflowMetrics metrics;

    /**
     * Handle workflow started event
     */
    @EventListener
    public void onWorkflowStarted(WorkflowStartedEvent event) {
        log.info("Workflow started: {} (instance: {})", event.workflowName(), event.instanceId());
        metrics.recordWorkflowStarted(event.workflowName());
    }

    /**
     * Handle workflow completed event
     */
    @EventListener
    public void onWorkflowCompleted(WorkflowCompletedEvent event) {
        log.info("Workflow completed: {} (instance: {}) in {}",
            event.workflowName(), event.instanceId(), event.duration());

        if (event.duration() != null) {
            metrics.recordWorkflowCompleted(event.workflowName(), event.duration());
        }
    }

    /**
     * Handle workflow failed event
     */
    @EventListener
    public void onWorkflowFailed(WorkflowFailedEvent event) {
        log.error("Workflow failed: {} (instance: {}) - Reason: {} - Failed steps: {}",
            event.workflowName(), event.instanceId(), event.reason(), event.failedSteps());

        metrics.recordWorkflowFailed(event.workflowName(), event.reason());
    }

    /**
     * Handle workflow paused event
     */
    @EventListener
    public void onWorkflowPaused(WorkflowPausedEvent event) {
        log.info("Workflow paused: {} (instance: {})", event.workflowName(), event.instanceId());
    }

    /**
     * Handle workflow resumed event
     */
    @EventListener
    public void onWorkflowResumed(WorkflowResumedEvent event) {
        log.info("Workflow resumed: {} (instance: {})", event.workflowName(), event.instanceId());
    }

    /**
     * Handle workflow cancelled event
     */
    @EventListener
    public void onWorkflowCancelled(WorkflowCancelledEvent event) {
        log.warn("Workflow cancelled: {} (instance: {})", event.workflowName(), event.instanceId());
        metrics.recordWorkflowCancelled(event.workflowName());
    }

    /**
     * Handle compensation started event
     */
    @EventListener
    public void onCompensationStarted(CompensationStartedEvent event) {
        log.info("Compensation started: {} (instance: {}) - {} steps to compensate",
            event.workflowName(), event.instanceId(), event.stepsToCompensate());
    }

    /**
     * Handle compensation completed event
     */
    @EventListener
    public void onCompensationCompleted(CompensationCompletedEvent event) {
        log.info("Compensation completed: {} (instance: {})",
            event.workflowName(), event.instanceId());
        metrics.recordWorkflowCompensated(event.workflowName());
    }

    /**
     * Handle step started event
     */
    @EventListener
    public void onStepStarted(StepStartedEvent event) {
        log.debug("Step started: {} (instance: {})", event.stepId(), event.instanceId());
    }

    /**
     * Handle step completed event
     */
    @EventListener
    public void onStepCompleted(StepCompletedEvent event) {
        log.info("Step completed: {} (instance: {}) in {}",
            event.stepId(), event.instanceId(), event.duration());

        if (event.duration() != null) {
            // Note: we don't have workflow name here, would need to fetch from instance
            metrics.recordStepExecuted("unknown", event.stepId(), event.duration(),
                fr.postiqa.core.domain.workflow.enums.StepStatus.COMPLETED);
        }
    }

    /**
     * Handle step failed event
     */
    @EventListener
    public void onStepFailed(StepFailedEvent event) {
        log.error("Step failed: {} (instance: {}) - Error: {} - Attempt: {}",
            event.stepId(), event.instanceId(), event.errorMessage(), event.attemptNumber());
    }

    /**
     * Handle step retried event
     */
    @EventListener
    public void onStepRetried(StepRetriedEvent event) {
        log.warn("Step retried: {} (instance: {}) - Attempt: {}",
            event.stepId(), event.instanceId(), event.attemptNumber());

        metrics.recordStepRetried("unknown", event.stepId(), event.attemptNumber());
    }

    /**
     * Handle step skipped event
     */
    @EventListener
    public void onStepSkipped(StepSkippedEvent event) {
        log.info("Step skipped: {} (instance: {})", event.stepId(), event.instanceId());
    }

    /**
     * Handle step compensated event
     */
    @EventListener
    public void onStepCompensated(StepCompensatedEvent event) {
        log.info("Step compensated: {} (instance: {})", event.stepId(), event.instanceId());
        metrics.recordStepCompensated("unknown", event.stepId());
    }
}
