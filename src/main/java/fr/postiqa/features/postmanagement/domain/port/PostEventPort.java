package fr.postiqa.features.postmanagement.domain.port;

import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.domain.vo.PostId;

import java.time.Instant;
import java.util.List;

/**
 * Port for publishing post-related events.
 * Implemented by event adapters (Spring Modulith, messaging, etc.).
 */
public interface PostEventPort {

    /**
     * Event: Post was created
     */
    record PostCreatedEvent(
        PostId postId,
        List<ChannelId> channelIds,
        Instant createdAt
    ) {}

    /**
     * Event: Post was scheduled
     */
    record PostScheduledEvent(
        PostId postId,
        List<ChannelId> channelIds,
        Instant scheduledFor
    ) {}

    /**
     * Event: Post scheduled was cancelled
     */
    record PostScheduleCancelledEvent(
        PostId postId,
        Instant cancelledAt
    ) {}

    /**
     * Event: Post was published
     */
    record PostPublishedEvent(
        PostId postId,
        ChannelId channelId,
        String externalPostId,
        Instant publishedAt
    ) {}

    /**
     * Event: Post publication failed
     */
    record PostPublishFailedEvent(
        PostId postId,
        ChannelId channelId,
        String errorMessage,
        Instant failedAt
    ) {}

    /**
     * Event: Post was deleted
     */
    record PostDeletedEvent(
        PostId postId,
        Instant deletedAt
    ) {}

    /**
     * Publish post created event
     */
    void publishPostCreated(PostCreatedEvent event);

    /**
     * Publish post scheduled event
     */
    void publishPostScheduled(PostScheduledEvent event);

    /**
     * Publish post schedule cancelled event
     */
    void publishPostScheduleCancelled(PostScheduleCancelledEvent event);

    /**
     * Publish post published event
     */
    void publishPostPublished(PostPublishedEvent event);

    /**
     * Publish post publish failed event
     */
    void publishPostPublishFailed(PostPublishFailedEvent event);

    /**
     * Publish post deleted event
     */
    void publishPostDeleted(PostDeletedEvent event);
}
