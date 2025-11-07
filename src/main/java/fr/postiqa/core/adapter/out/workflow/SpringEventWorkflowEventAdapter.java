package fr.postiqa.core.adapter.out.workflow;

import fr.postiqa.core.domain.workflow.model.StepResult;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring Event implementation of WorkflowEventPort.
 * Publishes workflow events using Spring's ApplicationEventPublisher.
 * These events can be consumed by other modules via @EventListener or @ApplicationModuleListener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventWorkflowEventAdapter implements WorkflowEventPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishWorkflowStarted(WorkflowInstance instance) {
        log.debug("Publishing WorkflowStarted event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new WorkflowStartedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName(),
            instance.getCreatedAt()
        ));
    }

    @Override
    public void publishWorkflowCompleted(WorkflowInstance instance) {
        log.debug("Publishing WorkflowCompleted event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new WorkflowCompletedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName(),
            instance.getDuration().orElse(null)
        ));
    }

    @Override
    public void publishWorkflowFailed(WorkflowInstance instance, String reason) {
        log.debug("Publishing WorkflowFailed event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new WorkflowFailedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName(),
            reason,
            instance.getFailedStepIds()
        ));
    }

    @Override
    public void publishWorkflowPaused(WorkflowInstance instance) {
        log.debug("Publishing WorkflowPaused event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new WorkflowPausedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName()
        ));
    }

    @Override
    public void publishWorkflowResumed(WorkflowInstance instance) {
        log.debug("Publishing WorkflowResumed event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new WorkflowResumedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName()
        ));
    }

    @Override
    public void publishWorkflowCancelled(WorkflowInstance instance) {
        log.debug("Publishing WorkflowCancelled event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new WorkflowCancelledEvent(
            instance.getInstanceId(),
            instance.getWorkflowName()
        ));
    }

    @Override
    public void publishCompensationStarted(WorkflowInstance instance) {
        log.debug("Publishing CompensationStarted event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new CompensationStartedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName(),
            instance.getCompletedStepIds().size()
        ));
    }

    @Override
    public void publishCompensationCompleted(WorkflowInstance instance) {
        log.debug("Publishing CompensationCompleted event for instance: {}", instance.getInstanceId());
        eventPublisher.publishEvent(new CompensationCompletedEvent(
            instance.getInstanceId(),
            instance.getWorkflowName()
        ));
    }

    @Override
    public void publishStepStarted(String instanceId, String stepId) {
        log.debug("Publishing StepStarted event for step: {} in instance: {}", stepId, instanceId);
        eventPublisher.publishEvent(new StepStartedEvent(instanceId, stepId));
    }

    @Override
    public void publishStepCompleted(String instanceId, String stepId, StepResult<?> result) {
        log.debug("Publishing StepCompleted event for step: {}", stepId);
        eventPublisher.publishEvent(new StepCompletedEvent(
            instanceId,
            stepId,
            result.duration()
        ));
    }

    @Override
    public void publishStepFailed(String instanceId, String stepId, StepResult<?> result) {
        log.debug("Publishing StepFailed event for step: {}", stepId);
        eventPublisher.publishEvent(new StepFailedEvent(
            instanceId,
            stepId,
            result.errorMessage().orElse("Unknown error"),
            result.attemptNumber()
        ));
    }

    @Override
    public void publishStepRetried(String instanceId, String stepId, int attemptNumber) {
        log.debug("Publishing StepRetried event for step: {} (attempt: {})", stepId, attemptNumber);
        eventPublisher.publishEvent(new StepRetriedEvent(instanceId, stepId, attemptNumber));
    }

    @Override
    public void publishStepSkipped(String instanceId, String stepId) {
        log.debug("Publishing StepSkipped event for step: {}", stepId);
        eventPublisher.publishEvent(new StepSkippedEvent(instanceId, stepId));
    }

    @Override
    public void publishStepCompensated(String instanceId, String stepId) {
        log.debug("Publishing StepCompensated event for step: {}", stepId);
        eventPublisher.publishEvent(new StepCompensatedEvent(instanceId, stepId));
    }

    // Event record definitions

    public record WorkflowStartedEvent(String instanceId, String workflowName, java.time.Instant startedAt) {}
    public record WorkflowCompletedEvent(String instanceId, String workflowName, java.time.Duration duration) {}
    public record WorkflowFailedEvent(String instanceId, String workflowName, String reason, java.util.Set<String> failedSteps) {}
    public record WorkflowPausedEvent(String instanceId, String workflowName) {}
    public record WorkflowResumedEvent(String instanceId, String workflowName) {}
    public record WorkflowCancelledEvent(String instanceId, String workflowName) {}
    public record CompensationStartedEvent(String instanceId, String workflowName, int stepsToCompensate) {}
    public record CompensationCompletedEvent(String instanceId, String workflowName) {}
    public record StepStartedEvent(String instanceId, String stepId) {}
    public record StepCompletedEvent(String instanceId, String stepId, java.time.Duration duration) {}
    public record StepFailedEvent(String instanceId, String stepId, String errorMessage, int attemptNumber) {}
    public record StepRetriedEvent(String instanceId, String stepId, int attemptNumber) {}
    public record StepSkippedEvent(String instanceId, String stepId) {}
    public record StepCompensatedEvent(String instanceId, String stepId) {}
}
