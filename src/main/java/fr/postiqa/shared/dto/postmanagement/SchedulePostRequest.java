package fr.postiqa.shared.dto.postmanagement;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for scheduling a post.
 */
public record SchedulePostRequest(
    @NotNull(message = "Scheduled time cannot be null")
    @Future(message = "Scheduled time must be in the future")
    Instant scheduledFor
) {}
