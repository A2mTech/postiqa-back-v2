package fr.postiqa.core.domain.workflow.model;

import fr.postiqa.core.domain.workflow.enums.StepStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Result of a workflow step execution.
 * Encapsulates the output data, status, execution metadata, and any error.
 *
 * @param <O> The output type of the step
 */
public record StepResult<O>(
    StepStatus status,
    Optional<O> output,
    Optional<Exception> error,
    Instant startTime,
    Instant endTime,
    int attemptNumber,
    Optional<String> errorMessage
) {

    /**
     * Create a successful step result
     */
    public static <O> StepResult<O> success(O output, Instant startTime, Instant endTime, int attemptNumber) {
        return new StepResult<>(
            StepStatus.COMPLETED,
            Optional.of(output),
            Optional.empty(),
            startTime,
            endTime,
            attemptNumber,
            Optional.empty()
        );
    }

    /**
     * Create a successful step result with no output
     */
    public static <O> StepResult<O> success(Instant startTime, Instant endTime, int attemptNumber) {
        return new StepResult<>(
            StepStatus.COMPLETED,
            Optional.empty(),
            Optional.empty(),
            startTime,
            endTime,
            attemptNumber,
            Optional.empty()
        );
    }

    /**
     * Create a failed step result
     */
    public static <O> StepResult<O> failure(Exception error, Instant startTime, Instant endTime, int attemptNumber) {
        return new StepResult<>(
            StepStatus.FAILED,
            Optional.empty(),
            Optional.of(error),
            startTime,
            endTime,
            attemptNumber,
            Optional.of(error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName())
        );
    }

    /**
     * Create a timed out step result
     */
    public static <O> StepResult<O> timedOut(Instant startTime, Instant endTime, int attemptNumber) {
        return new StepResult<>(
            StepStatus.TIMED_OUT,
            Optional.empty(),
            Optional.empty(),
            startTime,
            endTime,
            attemptNumber,
            Optional.of("Step execution timed out")
        );
    }

    /**
     * Create a skipped step result
     */
    public static <O> StepResult<O> skipped(Instant timestamp) {
        return new StepResult<>(
            StepStatus.SKIPPED,
            Optional.empty(),
            Optional.empty(),
            timestamp,
            timestamp,
            0,
            Optional.of("Step was skipped")
        );
    }

    /**
     * Create a compensated step result
     */
    public static <O> StepResult<O> compensated(Instant startTime, Instant endTime) {
        return new StepResult<>(
            StepStatus.COMPENSATED,
            Optional.empty(),
            Optional.empty(),
            startTime,
            endTime,
            0,
            Optional.of("Step was compensated")
        );
    }

    /**
     * Check if step execution was successful
     */
    public boolean isSuccess() {
        return status == StepStatus.COMPLETED;
    }

    /**
     * Check if step execution failed
     */
    public boolean isFailure() {
        return status == StepStatus.FAILED || status == StepStatus.TIMED_OUT;
    }

    /**
     * Calculate execution duration
     */
    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    /**
     * Get the output or throw if not present
     */
    public O getOutputOrThrow() {
        return output.orElseThrow(() ->
            new IllegalStateException("Step result does not contain output")
        );
    }

    /**
     * Get the error or throw if not present
     */
    public Exception getErrorOrThrow() {
        return error.orElseThrow(() ->
            new IllegalStateException("Step result does not contain an error")
        );
    }
}
