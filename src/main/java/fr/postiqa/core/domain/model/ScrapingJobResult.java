package fr.postiqa.core.domain.model;

import fr.postiqa.core.domain.enums.JobStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Represents the result of an async scraping job
 */
public record ScrapingJobResult<T>(
    String jobId,
    JobStatus status,
    T result,
    String errorMessage,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Map<String, Object> jobMetadata
) {
    public ScrapingJobResult {
        // Ensure jobMetadata is never null
        jobMetadata = jobMetadata != null ? Map.copyOf(jobMetadata) : Collections.emptyMap();
    }

    public boolean isComplete() {
        return status != null && status.isTerminal();
    }

    public boolean isSuccess() {
        return status != null && status.isSuccess();
    }

    public boolean isFailure() {
        return status != null && status.isFailure();
    }

    public boolean isInProgress() {
        return status != null && status.isInProgress();
    }

    public Duration duration() {
        if (startedAt == null || finishedAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, finishedAt);
    }

    public static <T> ScrapingJobResult<T> running(String jobId, LocalDateTime startedAt) {
        return new ScrapingJobResult<>(
            jobId,
            JobStatus.RUNNING,
            null,
            null,
            startedAt,
            null,
            Collections.emptyMap()
        );
    }

    public static <T> ScrapingJobResult<T> success(String jobId, T result, LocalDateTime startedAt, LocalDateTime finishedAt) {
        return new ScrapingJobResult<>(
            jobId,
            JobStatus.SUCCEEDED,
            result,
            null,
            startedAt,
            finishedAt,
            Collections.emptyMap()
        );
    }

    public static <T> ScrapingJobResult<T> failure(String jobId, String errorMessage, LocalDateTime startedAt, LocalDateTime finishedAt) {
        return new ScrapingJobResult<>(
            jobId,
            JobStatus.FAILED,
            null,
            errorMessage,
            startedAt,
            finishedAt,
            Collections.emptyMap()
        );
    }
}
