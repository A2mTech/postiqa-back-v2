package fr.postiqa.shared.dto.postmanagement;

import fr.postiqa.shared.enums.PostChannelStatus;
import fr.postiqa.shared.enums.SocialPlatform;

import java.time.Instant;

/**
 * Response DTO for channel assignment (post-channel relationship).
 */
public record ChannelAssignmentDto(
    String channelId,
    String channelName,
    SocialPlatform platform,
    PostChannelStatus status,
    String externalPostId,
    Instant publishedAt,
    String errorMessage
) {}
