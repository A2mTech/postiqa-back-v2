package fr.postiqa.features.postmanagement.domain.model;

import fr.postiqa.features.postmanagement.domain.vo.*;
import fr.postiqa.shared.enums.PostStatus;
import fr.postiqa.shared.enums.PostType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Post aggregate root representing a social media post.
 * Contains all business logic for post lifecycle management.
 * Supports cross-posting to multiple channels/platforms.
 */
public class Post {
    private final PostId id;
    private final UserId createdBy;
    private Content content;
    private final List<Media> media;
    private PostStatus status;
    private final PostType type;
    private ScheduleInfo scheduleInfo;
    private final List<ChannelAssignment> channelAssignments;
    private final Instant createdAt;
    private Instant updatedAt;

    // Private constructor for domain control
    private Post(
        PostId id,
        UserId createdBy,
        Content content,
        List<Media> media,
        PostStatus status,
        PostType type,
        ScheduleInfo scheduleInfo,
        List<ChannelAssignment> channelAssignments,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.createdBy = createdBy;
        this.content = content;
        this.media = new ArrayList<>(media);
        this.status = status;
        this.type = type;
        this.scheduleInfo = scheduleInfo;
        this.channelAssignments = new ArrayList<>(channelAssignments);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Create a new draft post
     */
    public static Post createDraft(UserId createdBy, Content content, List<ChannelId> channelIds) {
        if (createdBy == null) {
            throw new IllegalArgumentException("Creator user ID cannot be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (channelIds == null || channelIds.isEmpty()) {
            throw new IllegalArgumentException("At least one channel must be selected");
        }

        PostId postId = PostId.generate();
        List<ChannelAssignment> assignments = channelIds.stream()
            .map(ChannelAssignment::pending)
            .toList();

        Instant now = Instant.now();
        return new Post(
            postId,
            createdBy,
            content,
            new ArrayList<>(),
            PostStatus.DRAFT,
            PostType.MANUAL,
            ScheduleInfo.immediate(),
            assignments,
            now,
            now
        );
    }

    /**
     * Create a new generated post (from AI)
     */
    public static Post createGenerated(UserId createdBy, Content content, List<ChannelId> channelIds) {
        if (createdBy == null) {
            throw new IllegalArgumentException("Creator user ID cannot be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (channelIds == null || channelIds.isEmpty()) {
            throw new IllegalArgumentException("At least one channel must be selected");
        }

        PostId postId = PostId.generate();
        List<ChannelAssignment> assignments = channelIds.stream()
            .map(ChannelAssignment::pending)
            .toList();

        Instant now = Instant.now();
        return new Post(
            postId,
            createdBy,
            content,
            new ArrayList<>(),
            PostStatus.DRAFT,
            PostType.GENERATED,
            ScheduleInfo.immediate(),
            assignments,
            now,
            now
        );
    }

    /**
     * Reconstitute a post from persistence (for use by repository)
     */
    public static Post reconstitute(
        PostId id,
        UserId createdBy,
        Content content,
        List<Media> media,
        PostStatus status,
        PostType type,
        ScheduleInfo scheduleInfo,
        List<ChannelAssignment> channelAssignments,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Post(id, createdBy, content, media, status, type, scheduleInfo, channelAssignments, createdAt, updatedAt);
    }

    /**
     * Update post content
     */
    public void updateContent(Content newContent) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot edit post in status: " + status);
        }
        if (newContent == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        this.content = newContent;
        this.updatedAt = Instant.now();
    }

    /**
     * Add media to post
     */
    public void addMedia(Media media) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot add media to post in status: " + status);
        }
        if (media == null) {
            throw new IllegalArgumentException("Media cannot be null");
        }
        this.media.add(media);
        this.updatedAt = Instant.now();
    }

    /**
     * Remove media from post
     */
    public void removeMedia(MediaId mediaId) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot remove media from post in status: " + status);
        }
        if (mediaId == null) {
            throw new IllegalArgumentException("Media ID cannot be null");
        }
        this.media.removeIf(m -> m.id().equals(mediaId));
        this.updatedAt = Instant.now();
    }

    /**
     * Schedule post for future publishing
     */
    public void schedule(Instant scheduledFor) {
        if (!status.canBeScheduled()) {
            throw new IllegalStateException("Cannot schedule post in status: " + status);
        }
        if (scheduledFor == null) {
            throw new IllegalArgumentException("Scheduled time cannot be null");
        }
        if (scheduledFor.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Cannot schedule post in the past");
        }
        if (channelAssignments.isEmpty()) {
            throw new IllegalStateException("Cannot schedule post without channels");
        }

        this.scheduleInfo = ScheduleInfo.scheduledFor(scheduledFor);
        this.status = PostStatus.SCHEDULED;
        this.updatedAt = Instant.now();
    }

    /**
     * Cancel scheduled post
     */
    public void cancelSchedule() {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel post in status: " + status);
        }

        this.scheduleInfo = this.scheduleInfo.cancel();
        this.status = PostStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Add channel to post
     */
    public void addChannel(ChannelId channelId) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot add channel to post in status: " + status);
        }
        if (channelId == null) {
            throw new IllegalArgumentException("Channel ID cannot be null");
        }
        if (hasChannel(channelId)) {
            throw new IllegalArgumentException("Post already assigned to this channel");
        }

        this.channelAssignments.add(ChannelAssignment.pending(channelId));
        this.updatedAt = Instant.now();
    }

    /**
     * Remove channel from post
     */
    public void removeChannel(ChannelId channelId) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot remove channel from post in status: " + status);
        }
        if (channelId == null) {
            throw new IllegalArgumentException("Channel ID cannot be null");
        }

        this.channelAssignments.removeIf(ca -> ca.channelId().equals(channelId));
        this.updatedAt = Instant.now();

        if (this.channelAssignments.isEmpty()) {
            throw new IllegalStateException("Post must have at least one channel");
        }
    }

    /**
     * Start publishing process
     */
    public void startPublishing() {
        if (status != PostStatus.SCHEDULED && status != PostStatus.DRAFT) {
            throw new IllegalStateException("Cannot start publishing from status: " + status);
        }
        if (channelAssignments.isEmpty()) {
            throw new IllegalStateException("Cannot publish post without channels");
        }

        this.status = PostStatus.PUBLISHING;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark a channel as publishing
     */
    public void markChannelAsPublishing(ChannelId channelId) {
        ChannelAssignment assignment = findChannelAssignment(channelId);
        int index = channelAssignments.indexOf(assignment);
        channelAssignments.set(index, assignment.startPublishing());
        this.updatedAt = Instant.now();
    }

    /**
     * Mark a channel as published
     */
    public void markChannelAsPublished(ChannelId channelId, String externalPostId, Instant publishedAt) {
        ChannelAssignment assignment = findChannelAssignment(channelId);
        int index = channelAssignments.indexOf(assignment);
        channelAssignments.set(index, assignment.markAsPublished(externalPostId, publishedAt));
        this.updatedAt = Instant.now();

        // If all channels are published, mark post as published
        if (allChannelsPublished()) {
            this.status = PostStatus.PUBLISHED;
        }
    }

    /**
     * Mark a channel as failed
     */
    public void markChannelAsFailed(ChannelId channelId, String errorMessage) {
        ChannelAssignment assignment = findChannelAssignment(channelId);
        int index = channelAssignments.indexOf(assignment);
        channelAssignments.set(index, assignment.markAsFailed(errorMessage));
        this.updatedAt = Instant.now();

        // If all channels failed, mark post as failed
        if (allChannelsFailed()) {
            this.status = PostStatus.FAILED;
        }
    }

    // Getters

    public PostId getId() {
        return id;
    }

    public UserId getCreatedBy() {
        return createdBy;
    }

    public Content getContent() {
        return content;
    }

    public List<Media> getMedia() {
        return Collections.unmodifiableList(media);
    }

    public PostStatus getStatus() {
        return status;
    }

    public PostType getType() {
        return type;
    }

    public ScheduleInfo getScheduleInfo() {
        return scheduleInfo;
    }

    public List<ChannelAssignment> getChannelAssignments() {
        return Collections.unmodifiableList(channelAssignments);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Business logic queries

    public boolean isEditable() {
        return status.isEditable();
    }

    public boolean canBeScheduled() {
        return status.canBeScheduled();
    }

    public boolean canBeCancelled() {
        return status.canBeCancelled();
    }

    public boolean isScheduled() {
        return status == PostStatus.SCHEDULED && scheduleInfo.isScheduled();
    }

    public boolean isReadyToPublish() {
        return isScheduled() && scheduleInfo.isReadyToPublish();
    }

    public boolean hasMedia() {
        return !media.isEmpty();
    }

    public boolean hasChannel(ChannelId channelId) {
        return channelAssignments.stream()
            .anyMatch(ca -> ca.channelId().equals(channelId));
    }

    public boolean allChannelsPublished() {
        return !channelAssignments.isEmpty() &&
               channelAssignments.stream().allMatch(ChannelAssignment::isPublished);
    }

    public boolean allChannelsFailed() {
        return !channelAssignments.isEmpty() &&
               channelAssignments.stream().allMatch(ca -> ca.status() == fr.postiqa.shared.enums.PostChannelStatus.FAILED);
    }

    private ChannelAssignment findChannelAssignment(ChannelId channelId) {
        return channelAssignments.stream()
            .filter(ca -> ca.channelId().equals(channelId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Channel not assigned to this post: " + channelId));
    }
}
