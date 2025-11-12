package fr.postiqa.shared.usecase;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds execution context and metadata for a use case execution.
 * This class is used by the UseCaseHandler aspect to track execution details
 * for logging, audit trail, and performance monitoring.
 */
@Data
@Builder
public class UseCaseExecutionContext {

    /**
     * Unique identifier for this execution
     */
    private final UUID executionId;

    /**
     * Name of the use case being executed
     */
    private final String useCaseName;

    /**
     * Resource type the use case operates on (e.g., "POST", "MEMBER")
     */
    private final String resourceType;

    /**
     * ID of the resource being operated on (extracted from input/output)
     */
    private UUID resourceId;

    /**
     * User ID from tenant context
     */
    private UUID userId;

    /**
     * Organization ID from tenant context
     */
    private UUID organizationId;

    /**
     * Client ID from tenant context (agency mode)
     */
    private UUID clientId;

    /**
     * Client IP address
     */
    private String ipAddress;

    /**
     * User agent from HTTP request
     */
    private String userAgent;

    /**
     * When the execution started
     */
    private final Instant startTime;

    /**
     * When the execution ended
     */
    private Instant endTime;

    /**
     * Duration of the execution
     */
    private Duration duration;

    /**
     * Whether the execution was successful
     */
    private boolean success;

    /**
     * Exception thrown during execution (if any)
     */
    private Throwable error;

    /**
     * Action name for activity log (derived from use case name)
     */
    private String action;

    /**
     * Additional metadata for audit trail
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Mark the execution as completed successfully
     */
    public void markSuccess() {
        this.endTime = Instant.now();
        this.duration = Duration.between(startTime, endTime);
        this.success = true;
    }

    /**
     * Mark the execution as failed with an error
     */
    public void markFailure(Throwable error) {
        this.endTime = Instant.now();
        this.duration = Duration.between(startTime, endTime);
        this.success = false;
        this.error = error;
    }

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * Add multiple metadata entries
     */
    public void addMetadata(Map<String, Object> entries) {
        this.metadata.putAll(entries);
    }

    /**
     * Get execution duration in milliseconds
     */
    public long getDurationMillis() {
        return duration != null ? duration.toMillis() : 0L;
    }

    /**
     * Get error message if execution failed
     */
    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    /**
     * Get error class name if execution failed
     */
    public String getErrorType() {
        return error != null ? error.getClass().getSimpleName() : null;
    }
}
