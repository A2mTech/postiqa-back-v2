package fr.postiqa.core.domain.workflow.model;

import fr.postiqa.core.domain.workflow.enums.StepStatus;
import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Runtime instance of a workflow execution.
 * Tracks the state, progress, and execution history of a workflow.
 * Mutable by design (represents runtime state).
 */
public class WorkflowInstance {

    private final String instanceId;
    private final String workflowName;
    private final Instant createdAt;
    private WorkflowStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private WorkflowContext context;
    private final Map<String, StepExecution> stepExecutions;
    private Optional<String> failureReason;

    /**
     * Constructor for creating a new workflow instance
     */
    public WorkflowInstance(
        String instanceId,
        String workflowName,
        WorkflowContext initialContext
    ) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId cannot be null");
        this.workflowName = Objects.requireNonNull(workflowName, "workflowName cannot be null");
        this.createdAt = Instant.now();
        this.status = WorkflowStatus.PENDING;
        this.context = Objects.requireNonNull(initialContext, "initialContext cannot be null");
        this.stepExecutions = new LinkedHashMap<>();
        this.failureReason = Optional.empty();
    }

    /**
     * Start the workflow execution
     */
    public void start() {
        if (status != WorkflowStatus.PENDING && status != WorkflowStatus.PAUSED) {
            throw new IllegalStateException(
                String.format("Cannot start workflow in status %s", status)
            );
        }
        this.status = WorkflowStatus.RUNNING;
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    /**
     * Mark workflow as completed
     */
    public void complete() {
        if (status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                String.format("Cannot complete workflow in status %s", status)
            );
        }
        this.status = WorkflowStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark workflow as failed
     */
    public void fail(String reason) {
        this.status = WorkflowStatus.FAILED;
        this.completedAt = Instant.now();
        this.failureReason = Optional.of(reason);
    }

    /**
     * Pause workflow execution
     */
    public void pause() {
        if (!status.canPause()) {
            throw new IllegalStateException(
                String.format("Cannot pause workflow in status %s", status)
            );
        }
        this.status = WorkflowStatus.PAUSED;
    }

    /**
     * Resume workflow execution
     */
    public void resume() {
        if (!status.canResume()) {
            throw new IllegalStateException(
                String.format("Cannot resume workflow in status %s", status)
            );
        }
        this.status = WorkflowStatus.RUNNING;
    }

    /**
     * Start compensation (rollback)
     */
    public void startCompensation() {
        if (!status.canCompensate()) {
            throw new IllegalStateException(
                String.format("Cannot compensate workflow in status %s", status)
            );
        }
        this.status = WorkflowStatus.COMPENSATING;
    }

    /**
     * Mark compensation as complete
     */
    public void completeCompensation() {
        if (status != WorkflowStatus.COMPENSATING) {
            throw new IllegalStateException(
                String.format("Cannot complete compensation in status %s", status)
            );
        }
        this.status = WorkflowStatus.COMPENSATED;
        this.completedAt = Instant.now();
    }

    /**
     * Cancel the workflow
     */
    public void cancel() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                String.format("Cannot cancel workflow in terminal status %s", status)
            );
        }
        this.status = WorkflowStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    /**
     * Update the workflow context
     */
    public void updateContext(WorkflowContext newContext) {
        this.context = Objects.requireNonNull(newContext, "context cannot be null");
    }

    /**
     * Record a step execution
     */
    public void recordStepExecution(String stepId, StepExecution execution) {
        this.stepExecutions.put(stepId, execution);
    }

    /**
     * Get step execution by ID
     */
    public Optional<StepExecution> getStepExecution(String stepId) {
        return Optional.ofNullable(stepExecutions.get(stepId));
    }

    /**
     * Get all completed step IDs
     */
    public Set<String> getCompletedStepIds() {
        Set<String> completed = new HashSet<>();
        for (Map.Entry<String, StepExecution> entry : stepExecutions.entrySet()) {
            if (entry.getValue().status() == StepStatus.COMPLETED) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }

    /**
     * Get all failed step IDs
     */
    public Set<String> getFailedStepIds() {
        Set<String> failed = new HashSet<>();
        for (Map.Entry<String, StepExecution> entry : stepExecutions.entrySet()) {
            if (entry.getValue().status() == StepStatus.FAILED ||
                entry.getValue().status() == StepStatus.TIMED_OUT) {
                failed.add(entry.getKey());
            }
        }
        return failed;
    }

    /**
     * Calculate workflow progress (0.0 to 1.0)
     */
    public double getProgress(int totalSteps) {
        if (totalSteps == 0) {
            return 1.0;
        }
        long completedCount = stepExecutions.values().stream()
            .filter(exec -> exec.status() == StepStatus.COMPLETED)
            .count();
        return (double) completedCount / totalSteps;
    }

    /**
     * Calculate total execution duration
     */
    public Optional<Duration> getDuration() {
        if (startedAt == null) {
            return Optional.empty();
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return Optional.of(Duration.between(startedAt, endTime));
    }

    // Getters

    public String getInstanceId() {
        return instanceId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    public WorkflowContext getContext() {
        return context;
    }

    public Map<String, StepExecution> getStepExecutions() {
        return Collections.unmodifiableMap(stepExecutions);
    }

    public Optional<String> getFailureReason() {
        return failureReason;
    }

    /**
     * Nested record for step execution tracking
     */
    public record StepExecution(
        String stepId,
        StepStatus status,
        Instant startedAt,
        Instant completedAt,
        int attemptNumber,
        Optional<String> errorMessage,
        Optional<Object> output
    ) {
        public StepExecution {
            Objects.requireNonNull(stepId, "stepId cannot be null");
            Objects.requireNonNull(status, "status cannot be null");
            Objects.requireNonNull(startedAt, "startedAt cannot be null");
            Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
            Objects.requireNonNull(output, "output cannot be null");
        }

        public Optional<Duration> getDuration() {
            if (completedAt == null) {
                return Optional.empty();
            }
            return Optional.of(Duration.between(startedAt, completedAt));
        }

        public static StepExecution fromResult(String stepId, StepResult<?> result) {
            return new StepExecution(
                stepId,
                result.status(),
                result.startTime(),
                result.endTime(),
                result.attemptNumber(),
                result.errorMessage(),
                result.output().map(o -> (Object) o)
            );
        }
    }
}
