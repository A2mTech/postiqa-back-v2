package fr.postiqa.features.postmanagement.domain.vo;

import java.time.Instant;

/**
 * Value object representing post scheduling information.
 */
public record ScheduleInfo(
    Instant scheduledFor,
    boolean cancelled
) {

    public ScheduleInfo {
        if (scheduledFor != null && scheduledFor.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Scheduled time cannot be in the past");
        }
    }

    /**
     * Create schedule info for immediate publishing
     */
    public static ScheduleInfo immediate() {
        return new ScheduleInfo(null, false);
    }

    /**
     * Create schedule info for a specific date/time
     */
    public static ScheduleInfo scheduledFor(Instant scheduledFor) {
        if (scheduledFor == null) {
            throw new IllegalArgumentException("Scheduled time cannot be null");
        }
        return new ScheduleInfo(scheduledFor, false);
    }

    /**
     * Create cancelled schedule info
     */
    public static ScheduleInfo cancelled(Instant originallyScheduledFor) {
        return new ScheduleInfo(originallyScheduledFor, true);
    }

    /**
     * Check if post is scheduled
     */
    public boolean isScheduled() {
        return scheduledFor != null && !cancelled;
    }

    /**
     * Check if post should be published now
     */
    public boolean isReadyToPublish() {
        return isScheduled() && scheduledFor.isBefore(Instant.now());
    }

    /**
     * Check if scheduling is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancel the schedule
     */
    public ScheduleInfo cancel() {
        if (!isScheduled()) {
            throw new IllegalStateException("Cannot cancel a non-scheduled post");
        }
        return new ScheduleInfo(scheduledFor, true);
    }

    /**
     * Reschedule to a new time
     */
    public ScheduleInfo reschedule(Instant newScheduledFor) {
        if (newScheduledFor == null) {
            throw new IllegalArgumentException("New scheduled time cannot be null");
        }
        if (newScheduledFor.isBefore(Instant.now())) {
            throw new IllegalArgumentException("New scheduled time cannot be in the past");
        }
        return new ScheduleInfo(newScheduledFor, false);
    }
}
