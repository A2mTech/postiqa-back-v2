package fr.postiqa.core.infrastructure.workflow.state;

import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Manages workflow state transitions and validations.
 * Ensures state machine consistency and provides state analytics.
 */
@Component
@Slf4j
public class WorkflowStateManager {

    /**
     * Valid state transitions map
     */
    private static final Map<WorkflowStatus, Set<WorkflowStatus>> VALID_TRANSITIONS = Map.of(
        WorkflowStatus.PENDING, Set.of(WorkflowStatus.RUNNING, WorkflowStatus.CANCELLED),
        WorkflowStatus.RUNNING, Set.of(WorkflowStatus.PAUSED, WorkflowStatus.COMPLETED,
            WorkflowStatus.FAILED, WorkflowStatus.CANCELLED),
        WorkflowStatus.PAUSED, Set.of(WorkflowStatus.RUNNING, WorkflowStatus.CANCELLED),
        WorkflowStatus.FAILED, Set.of(WorkflowStatus.COMPENSATING),
        WorkflowStatus.COMPENSATING, Set.of(WorkflowStatus.COMPENSATED),
        WorkflowStatus.COMPLETED, Set.of(), // Terminal state
        WorkflowStatus.COMPENSATED, Set.of(), // Terminal state
        WorkflowStatus.CANCELLED, Set.of() // Terminal state
    );

    /**
     * Check if a state transition is valid
     */
    public boolean isValidTransition(WorkflowStatus from, WorkflowStatus to) {
        Set<WorkflowStatus> allowedTransitions = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        return allowedTransitions.contains(to);
    }

    /**
     * Validate a state transition or throw exception
     */
    public void validateTransition(WorkflowStatus from, WorkflowStatus to) {
        if (!isValidTransition(from, to)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid state transition from %s to %s", from, to)
            );
        }
    }

    /**
     * Get allowed next states for a workflow status
     */
    public Set<WorkflowStatus> getAllowedNextStates(WorkflowStatus current) {
        return VALID_TRANSITIONS.getOrDefault(current, Set.of());
    }

    /**
     * Check if workflow is in a recoverable state
     */
    public boolean isRecoverable(WorkflowInstance instance) {
        WorkflowStatus status = instance.getStatus();
        return status == WorkflowStatus.PAUSED || status == WorkflowStatus.FAILED;
    }

    /**
     * Check if workflow needs intervention
     */
    public boolean needsIntervention(WorkflowInstance instance) {
        return instance.getStatus() == WorkflowStatus.FAILED ||
               instance.getStatus() == WorkflowStatus.COMPENSATING;
    }

    /**
     * Calculate time in current state
     */
    public Duration getTimeInCurrentState(WorkflowInstance instance) {
        Instant stateChangeTime = switch (instance.getStatus()) {
            case PENDING -> instance.getCreatedAt();
            case RUNNING, PAUSED -> instance.getStartedAt().orElse(instance.getCreatedAt());
            case COMPLETED, FAILED, COMPENSATED, CANCELLED ->
                instance.getCompletedAt().orElse(Instant.now());
            case COMPENSATING -> instance.getCompletedAt().orElse(Instant.now());
        };

        return Duration.between(stateChangeTime, Instant.now());
    }

    /**
     * Get workflow state summary
     */
    public WorkflowStateSummary getStateSummary(WorkflowInstance instance) {
        return new WorkflowStateSummary(
            instance.getInstanceId(),
            instance.getWorkflowName(),
            instance.getStatus(),
            instance.getCreatedAt(),
            instance.getStartedAt().orElse(null),
            instance.getCompletedAt().orElse(null),
            instance.getDuration().orElse(null),
            getTimeInCurrentState(instance),
            instance.getFailureReason().orElse(null),
            isRecoverable(instance),
            needsIntervention(instance)
        );
    }

    /**
     * Get state transition history (would need to track in DB)
     */
    public List<StateTransition> getTransitionHistory(WorkflowInstance instance) {
        // This is a simplified version
        // In production, you'd track state transitions in a separate table
        List<StateTransition> transitions = new ArrayList<>();

        transitions.add(new StateTransition(
            null,
            WorkflowStatus.PENDING,
            instance.getCreatedAt(),
            "Workflow created"
        ));

        if (instance.getStartedAt().isPresent()) {
            transitions.add(new StateTransition(
                WorkflowStatus.PENDING,
                WorkflowStatus.RUNNING,
                instance.getStartedAt().get(),
                "Workflow started"
            ));
        }

        if (instance.getCompletedAt().isPresent()) {
            WorkflowStatus finalStatus = instance.getStatus();
            if (finalStatus.isTerminal()) {
                transitions.add(new StateTransition(
                    WorkflowStatus.RUNNING,
                    finalStatus,
                    instance.getCompletedAt().get(),
                    instance.getFailureReason().orElse("Workflow completed")
                ));
            }
        }

        return transitions;
    }

    /**
     * Check if workflow is healthy (no stuck state, no excessive failures)
     */
    public HealthStatus checkHealth(WorkflowInstance instance, Duration maxTimeInState) {
        // Check if stuck in non-terminal state
        if (!instance.getStatus().isTerminal()) {
            Duration timeInState = getTimeInCurrentState(instance);
            if (timeInState.compareTo(maxTimeInState) > 0) {
                return new HealthStatus(
                    false,
                    "Workflow appears stuck in state " + instance.getStatus(),
                    HealthLevel.WARNING
                );
            }
        }

        // Check failure state
        if (instance.getStatus() == WorkflowStatus.FAILED) {
            return new HealthStatus(
                false,
                "Workflow has failed: " + instance.getFailureReason().orElse("Unknown"),
                HealthLevel.CRITICAL
            );
        }

        // Check if compensating
        if (instance.getStatus() == WorkflowStatus.COMPENSATING) {
            return new HealthStatus(
                false,
                "Workflow is compensating (rollback in progress)",
                HealthLevel.WARNING
            );
        }

        return new HealthStatus(true, "Workflow is healthy", HealthLevel.OK);
    }

    /**
     * State summary record
     */
    public record WorkflowStateSummary(
        String instanceId,
        String workflowName,
        WorkflowStatus currentStatus,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Duration totalDuration,
        Duration timeInCurrentState,
        String failureReason,
        boolean isRecoverable,
        boolean needsIntervention
    ) {}

    /**
     * State transition record
     */
    public record StateTransition(
        WorkflowStatus fromStatus,
        WorkflowStatus toStatus,
        Instant transitionTime,
        String reason
    ) {}

    /**
     * Health status record
     */
    public record HealthStatus(
        boolean isHealthy,
        String message,
        HealthLevel level
    ) {}

    /**
     * Health level enum
     */
    public enum HealthLevel {
        OK,
        WARNING,
        CRITICAL
    }

    /**
     * Exception for invalid state transitions
     */
    public static class InvalidStateTransitionException extends RuntimeException {
        public InvalidStateTransitionException(String message) {
            super(message);
        }
    }
}
