package fr.postiqa.shared.dto.postmanagement;

import fr.postiqa.shared.enums.PostStatus;
import fr.postiqa.shared.enums.PostType;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a post.
 */
public record PostDto(
    String id,
    String createdBy,
    String content,
    List<String> hashtags,
    List<String> mentions,
    List<MediaDto> media,
    List<ChannelAssignmentDto> channels,
    PostStatus status,
    PostType type,
    Instant scheduledFor,
    Instant createdAt,
    Instant updatedAt
) {}
