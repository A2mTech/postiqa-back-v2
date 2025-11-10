package fr.postiqa.core.infrastructure.workflow.state;

import fr.postiqa.core.domain.workflow.enums.StepStatus;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks and analyzes the state of individual workflow steps.
 * Provides insights into step execution patterns and performance.
 */
@Component
@Slf4j
public class StepStateTracker {

    /**
     * Get all steps with a specific status
     */
    public Set<String> getStepsByStatus(WorkflowInstance instance, StepStatus status) {
        return instance.getStepExecutions().entrySet().stream()
            .filter(entry -> entry.getValue().status() == status)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Get steps that are currently executing
     */
    public Set<String> getRunningSteps(WorkflowInstance instance) {
        return getStepsByStatus(instance, StepStatus.RUNNING);
    }

    /**
     * Get steps that are pending execution
     */
    public Set<String> getPendingSteps(WorkflowInstance instance) {
        return getStepsByStatus(instance, StepStatus.PENDING);
    }

    /**
     * Calculate average step execution time
     */
    public Optional<Duration> getAverageStepDuration(WorkflowInstance instance) {
        List<Duration> durations = instance.getStepExecutions().values().stream()
            .filter(exec -> exec.status() == StepStatus.COMPLETED)
            .map(WorkflowInstance.StepExecution::getDuration)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        if (durations.isEmpty()) {
            return Optional.empty();
        }

        long averageMillis = durations.stream()
            .mapToLong(Duration::toMillis)
            .sum() / durations.size();

        return Optional.of(Duration.ofMillis(averageMillis));
    }

    /**
     * Get the slowest executed step
     */
    public Optional<StepExecutionSummary> getSlowestStep(WorkflowInstance instance) {
        return instance.getStepExecutions().values().stream()
            .filter(exec -> exec.status() == StepStatus.COMPLETED)
            .filter(exec -> exec.getDuration().isPresent())
            .max(Comparator.comparing(exec -> exec.getDuration().get()))
            .map(exec -> new StepExecutionSummary(
                exec.stepId(),
                exec.status(),
                exec.getDuration().orElse(Duration.ZERO),
                exec.attemptNumber(),
                exec.errorMessage()
            ));
    }

    /**
     * Get the fastest executed step
     */
    public Optional<StepExecutionSummary> getFastestStep(WorkflowInstance instance) {
        return instance.getStepExecutions().values().stream()
            .filter(exec -> exec.status() == StepStatus.COMPLETED)
            .filter(exec -> exec.getDuration().isPresent())
            .min(Comparator.comparing(exec -> exec.getDuration().get()))
            .map(exec -> new StepExecutionSummary(
                exec.stepId(),
                exec.status(),
                exec.getDuration().orElse(Duration.ZERO),
                exec.attemptNumber(),
                exec.errorMessage()
            ));
    }

    /**
     * Get all steps that required retries
     */
    public List<StepExecutionSummary> getRetriedSteps(WorkflowInstance instance) {
        return instance.getStepExecutions().values().stream()
            .filter(exec -> exec.attemptNumber() > 0)
            .map(exec -> new StepExecutionSummary(
                exec.stepId(),
                exec.status(),
                exec.getDuration().orElse(Duration.ZERO),
                exec.attemptNumber(),
                exec.errorMessage()
            ))
            .toList();
    }

    /**
     * Calculate retry rate (percentage of steps that needed retry)
     */
    public double getRetryRate(WorkflowInstance instance) {
        long totalSteps = instance.getStepExecutions().size();
        if (totalSteps == 0) {
            return 0.0;
        }

        long retriedSteps = instance.getStepExecutions().values().stream()
            .filter(exec -> exec.attemptNumber() > 0)
            .count();

        return (double) retriedSteps / totalSteps;
    }

    /**
     * Get execution statistics for a workflow
     */
    public WorkflowExecutionStats getExecutionStats(WorkflowInstance instance) {
        Map<StepStatus, Long> statusCounts = instance.getStepExecutions().values().stream()
            .collect(Collectors.groupingBy(
                WorkflowInstance.StepExecution::status,
                Collectors.counting()
            ));

        return new WorkflowExecutionStats(
            instance.getInstanceId(),
            instance.getStepExecutions().size(),
            statusCounts.getOrDefault(StepStatus.COMPLETED, 0L).intValue(),
            statusCounts.getOrDefault(StepStatus.FAILED, 0L).intValue()
                + statusCounts.getOrDefault(StepStatus.TIMED_OUT, 0L).intValue(),
            statusCounts.getOrDefault(StepStatus.RUNNING, 0L).intValue(),
            statusCounts.getOrDefault(StepStatus.PENDING, 0L).intValue(),
            getRetryRate(instance),
            getAverageStepDuration(instance)
        );
    }

    /**
     * Get timeline of step executions (ordered by start time)
     */
    public List<StepExecutionSummary> getExecutionTimeline(WorkflowInstance instance) {
        return instance.getStepExecutions().values().stream()
            .sorted(Comparator.comparing(WorkflowInstance.StepExecution::startedAt))
            .map(exec -> new StepExecutionSummary(
                exec.stepId(),
                exec.status(),
                exec.getDuration().orElse(Duration.ZERO),
                exec.attemptNumber(),
                exec.errorMessage()
            ))
            .toList();
    }

    /**
     * Check if workflow is stuck (has running steps but no recent progress)
     */
    public boolean isWorkflowStuck(WorkflowInstance instance, Duration threshold) {
        Set<String> runningSteps = getRunningSteps(instance);
        if (runningSteps.isEmpty()) {
            return false;
        }

        // Check if any running step has been running longer than threshold
        Instant now = Instant.now();
        return instance.getStepExecutions().values().stream()
            .filter(exec -> exec.status() == StepStatus.RUNNING)
            .anyMatch(exec -> Duration.between(exec.startedAt(), now).compareTo(threshold) > 0);
    }

    /**
     * Summary record for step execution
     */
    public record StepExecutionSummary(
        String stepId,
        StepStatus status,
        Duration duration,
        int attemptNumber,
        Optional<String> errorMessage
    ) {}

    /**
     * Statistics record for workflow execution
     */
    public record WorkflowExecutionStats(
        String instanceId,
        int totalSteps,
        int completedSteps,
        int failedSteps,
        int runningSteps,
        int pendingSteps,
        double retryRate,
        Optional<Duration> averageDuration
    ) {
        public double getCompletionRate() {
            if (totalSteps == 0) return 0.0;
            return (double) completedSteps / totalSteps;
        }

        public double getFailureRate() {
            if (totalSteps == 0) return 0.0;
            return (double) failedSteps / totalSteps;
        }
    }
}
