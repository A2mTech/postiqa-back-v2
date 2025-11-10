package fr.postiqa.core.infrastructure.workflow.monitoring;

import fr.postiqa.core.domain.workflow.enums.StepStatus;
import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Collects and publishes workflow execution metrics using Micrometer.
 * Provides observability for workflow performance and health.
 */
@Component
@Slf4j
public class WorkflowMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter workflowsStarted;
    private final Counter workflowsCompleted;
    private final Counter workflowsFailed;
    private final Counter workflowsCancelled;
    private final Counter workflowsCompensated;
    private final Counter stepsExecuted;
    private final Counter stepsFailed;
    private final Counter stepsRetried;
    private final Counter stepsCompensated;

    // Timers
    private final Timer workflowDuration;
    private final Timer stepDuration;

    public WorkflowMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.workflowsStarted = Counter.builder("workflow.started")
            .description("Number of workflows started")
            .register(meterRegistry);

        this.workflowsCompleted = Counter.builder("workflow.completed")
            .description("Number of workflows completed successfully")
            .register(meterRegistry);

        this.workflowsFailed = Counter.builder("workflow.failed")
            .description("Number of workflows that failed")
            .register(meterRegistry);

        this.workflowsCancelled = Counter.builder("workflow.cancelled")
            .description("Number of workflows cancelled")
            .register(meterRegistry);

        this.workflowsCompensated = Counter.builder("workflow.compensated")
            .description("Number of workflows compensated (rolled back)")
            .register(meterRegistry);

        this.stepsExecuted = Counter.builder("workflow.steps.executed")
            .description("Number of workflow steps executed")
            .register(meterRegistry);

        this.stepsFailed = Counter.builder("workflow.steps.failed")
            .description("Number of workflow steps that failed")
            .register(meterRegistry);

        this.stepsRetried = Counter.builder("workflow.steps.retried")
            .description("Number of workflow steps that were retried")
            .register(meterRegistry);

        this.stepsCompensated = Counter.builder("workflow.steps.compensated")
            .description("Number of workflow steps compensated")
            .register(meterRegistry);

        // Initialize timers
        this.workflowDuration = Timer.builder("workflow.duration")
            .description("Duration of workflow execution")
            .register(meterRegistry);

        this.stepDuration = Timer.builder("workflow.step.duration")
            .description("Duration of step execution")
            .register(meterRegistry);

        log.info("Workflow metrics initialized");
    }

    /**
     * Record workflow started
     */
    public void recordWorkflowStarted(String workflowName) {
        workflowsStarted.increment();
        log.debug("Recorded workflow started: {}", workflowName);
    }

    /**
     * Record workflow completion
     */
    public void recordWorkflowCompleted(String workflowName, Duration duration) {
        workflowsCompleted.increment();
        workflowDuration.record(duration);
        log.debug("Recorded workflow completed: {} in {}", workflowName, duration);
    }

    /**
     * Record workflow failure
     */
    public void recordWorkflowFailed(String workflowName, String reason) {
        workflowsFailed.increment();
        log.debug("Recorded workflow failed: {} - {}", workflowName, reason);
    }

    /**
     * Record workflow cancellation
     */
    public void recordWorkflowCancelled(String workflowName) {
        workflowsCancelled.increment();
        log.debug("Recorded workflow cancelled: {}", workflowName);
    }

    /**
     * Record workflow compensation
     */
    public void recordWorkflowCompensated(String workflowName) {
        workflowsCompensated.increment();
        log.debug("Recorded workflow compensated: {}", workflowName);
    }

    /**
     * Record step execution
     */
    public void recordStepExecuted(String workflowName, String stepId, Duration duration, StepStatus status) {
        stepsExecuted.increment();
        stepDuration.record(duration);

        if (status == StepStatus.FAILED || status == StepStatus.TIMED_OUT) {
            stepsFailed.increment();
        }

        log.debug("Recorded step executed: {}.{} in {} with status {}",
            workflowName, stepId, duration, status);
    }

    /**
     * Record step retry
     */
    public void recordStepRetried(String workflowName, String stepId, int attemptNumber) {
        stepsRetried.increment();
        log.debug("Recorded step retry: {}.{} (attempt {})", workflowName, stepId, attemptNumber);
    }

    /**
     * Record step compensation
     */
    public void recordStepCompensated(String workflowName, String stepId) {
        stepsCompensated.increment();
        log.debug("Recorded step compensated: {}.{}", workflowName, stepId);
    }

    /**
     * Record workflow status by type
     */
    public void recordWorkflowStatus(String workflowName, WorkflowStatus status) {
        // Note: gauges in Micrometer need a state object and function
        // For this simple case, we'll use a counter instead
        Counter.builder("workflow.status")
            .tag("workflow", workflowName)
            .tag("status", status.name())
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record active workflows gauge
     */
    public void recordActiveWorkflows(int count) {
        // Note: would need AtomicInteger state holder for true gauge
        // Simplified for now
        log.debug("Active workflows: {}", count);
    }

    /**
     * Record workflow by name
     */
    public void recordWorkflowByName(String workflowName, WorkflowStatus status, Duration duration) {
        Counter.builder("workflow.executions")
            .tag("workflow", workflowName)
            .tag("status", status.name())
            .description("Workflow executions by name and status")
            .register(meterRegistry)
            .increment();

        if (duration != null) {
            Timer.builder("workflow.duration.by.name")
                .tag("workflow", workflowName)
                .description("Workflow duration by name")
                .register(meterRegistry)
                .record(duration);
        }
    }

    /**
     * Record step by name and workflow
     */
    public void recordStepByName(String workflowName, String stepId, StepStatus status, Duration duration) {
        Counter.builder("workflow.step.executions")
            .tag("workflow", workflowName)
            .tag("step", stepId)
            .tag("status", status.name())
            .description("Step executions by workflow, step, and status")
            .register(meterRegistry)
            .increment();

        if (duration != null) {
            Timer.builder("workflow.step.duration.by.name")
                .tag("workflow", workflowName)
                .tag("step", stepId)
                .description("Step duration by workflow and step")
                .register(meterRegistry)
                .record(duration);
        }
    }

    /**
     * Get current metrics snapshot
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            (long) workflowsStarted.count(),
            (long) workflowsCompleted.count(),
            (long) workflowsFailed.count(),
            (long) workflowsCancelled.count(),
            (long) workflowsCompensated.count(),
            (long) stepsExecuted.count(),
            (long) stepsFailed.count(),
            (long) stepsRetried.count(),
            (long) stepsCompensated.count(),
            workflowDuration.mean(TimeUnit.MILLISECONDS),
            stepDuration.mean(TimeUnit.MILLISECONDS)
        );
    }

    /**
     * Metrics snapshot record
     */
    public record MetricsSnapshot(
        long workflowsStarted,
        long workflowsCompleted,
        long workflowsFailed,
        long workflowsCancelled,
        long workflowsCompensated,
        long stepsExecuted,
        long stepsFailed,
        long stepsRetried,
        long stepsCompensated,
        double averageWorkflowDurationMs,
        double averageStepDurationMs
    ) {
        public double getWorkflowSuccessRate() {
            long total = workflowsCompleted + workflowsFailed;
            if (total == 0) return 0.0;
            return (double) workflowsCompleted / total;
        }

        public double getStepSuccessRate() {
            long total = stepsExecuted;
            if (total == 0) return 0.0;
            return (double) (stepsExecuted - stepsFailed) / total;
        }

        public double getRetryRate() {
            if (stepsExecuted == 0) return 0.0;
            return (double) stepsRetried / stepsExecuted;
        }
    }
}
