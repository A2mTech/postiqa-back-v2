package fr.postiqa.features.postmanagement.domain.vo;

import fr.postiqa.shared.enums.PostChannelStatus;

import java.time.Instant;

/**
 * Value object representing the assignment of a post to a specific channel.
 * Tracks the publication status for each platform in cross-posting scenarios.
 */
public record ChannelAssignment(
    ChannelId channelId,
    PostChannelStatus status,
    String externalPostId,
    Instant publishedAt,
    String errorMessage
) {

    public ChannelAssignment {
        if (channelId == null) {
            throw new IllegalArgumentException("Channel ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (status == PostChannelStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("Published posts must have publishedAt timestamp");
        }
        if (status == PostChannelStatus.FAILED && (errorMessage == null || errorMessage.isBlank())) {
            throw new IllegalArgumentException("Failed posts must have an error message");
        }
    }

    /**
     * Create a pending channel assignment
     */
    public static ChannelAssignment pending(ChannelId channelId) {
        return new ChannelAssignment(channelId, PostChannelStatus.PENDING, null, null, null);
    }

    /**
     * Create a publishing channel assignment
     */
    public static ChannelAssignment publishing(ChannelId channelId) {
        return new ChannelAssignment(channelId, PostChannelStatus.PUBLISHING, null, null, null);
    }

    /**
     * Create a published channel assignment
     */
    public static ChannelAssignment published(ChannelId channelId, String externalPostId, Instant publishedAt) {
        if (externalPostId == null || externalPostId.isBlank()) {
            throw new IllegalArgumentException("External post ID cannot be null for published posts");
        }
        return new ChannelAssignment(channelId, PostChannelStatus.PUBLISHED, externalPostId, publishedAt, null);
    }

    /**
     * Create a failed channel assignment
     */
    public static ChannelAssignment failed(ChannelId channelId, String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("Error message cannot be null for failed posts");
        }
        return new ChannelAssignment(channelId, PostChannelStatus.FAILED, null, null, errorMessage);
    }

    /**
     * Create a cancelled channel assignment
     */
    public static ChannelAssignment cancelled(ChannelId channelId) {
        return new ChannelAssignment(channelId, PostChannelStatus.CANCELLED, null, null, null);
    }

    /**
     * Check if post has been published on this channel
     */
    public boolean isPublished() {
        return status.isPublished();
    }

    /**
     * Check if post is in a final state on this channel
     */
    public boolean isFinal() {
        return status.isFinal();
    }

    /**
     * Check if post can be retried on this channel
     */
    public boolean canRetry() {
        return status.canRetry();
    }

    /**
     * Check if post has an error on this channel
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isBlank();
    }

    /**
     * Transition to publishing state
     */
    public ChannelAssignment startPublishing() {
        if (status != PostChannelStatus.PENDING) {
            throw new IllegalStateException("Can only start publishing from PENDING state");
        }
        return new ChannelAssignment(channelId, PostChannelStatus.PUBLISHING, null, null, null);
    }

    /**
     * Transition to published state
     */
    public ChannelAssignment markAsPublished(String externalPostId, Instant publishedAt) {
        if (status != PostChannelStatus.PUBLISHING) {
            throw new IllegalStateException("Can only mark as published from PUBLISHING state");
        }
        return published(channelId, externalPostId, publishedAt);
    }

    /**
     * Transition to failed state
     */
    public ChannelAssignment markAsFailed(String errorMessage) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot fail a channel assignment in final state");
        }
        return failed(channelId, errorMessage);
    }

    /**
     * Transition to cancelled state
     */
    public ChannelAssignment markAsCancelled() {
        if (status == PostChannelStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot cancel a published post");
        }
        return cancelled(channelId);
    }

    /**
     * Retry a failed assignment
     */
    public ChannelAssignment retry() {
        if (!canRetry()) {
            throw new IllegalStateException("Cannot retry channel assignment in state: " + status);
        }
        return pending(channelId);
    }
}
