package fr.postiqa.core.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.postiqa.core.domain.enums.JobStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Apify run status response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApifyRunStatus(
    @JsonProperty("id") String id,
    @JsonProperty("actId") String actId,
    @JsonProperty("status") String status,
    @JsonProperty("startedAt") LocalDateTime startedAt,
    @JsonProperty("finishedAt") LocalDateTime finishedAt,
    @JsonProperty("defaultDatasetId") String defaultDatasetId,
    @JsonProperty("stats") Map<String, Object> stats,
    @JsonProperty("exitCode") Integer exitCode
) {
    /**
     * Convert Apify status to our JobStatus enum
     */
    public JobStatus toJobStatus() {
        return switch (status.toUpperCase()) {
            case "READY" -> JobStatus.READY;
            case "RUNNING" -> JobStatus.RUNNING;
            case "SUCCEEDED" -> JobStatus.SUCCEEDED;
            case "FAILED" -> JobStatus.FAILED;
            case "TIMING-OUT" -> JobStatus.TIMED_OUT;
            case "TIMED-OUT" -> JobStatus.TIMED_OUT;
            case "ABORTING" -> JobStatus.ABORTED;
            case "ABORTED" -> JobStatus.ABORTED;
            default -> JobStatus.FAILED;
        };
    }
}
