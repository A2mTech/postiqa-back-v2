package fr.postiqa.features.postmanagement.adapter.in.rest;

import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.ChannelRepositoryPort;
import fr.postiqa.features.postmanagement.domain.vo.ChannelAssignment;
import fr.postiqa.features.postmanagement.domain.vo.Media;
import fr.postiqa.shared.dto.postmanagement.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting domain models to REST DTOs.
 */
@Component
public class PostDtoMapper {

    private final ChannelRepositoryPort channelRepository;

    public PostDtoMapper(ChannelRepositoryPort channelRepository) {
        this.channelRepository = channelRepository;
    }

    /**
     * Convert Post domain model to PostDto
     */
    public PostDto toDto(Post post) {
        return new PostDto(
            post.getId().toString(),
            post.getCreatedBy().toString(),
            post.getContent().text(),
            post.getContent().hashtags(),
            post.getContent().mentions(),
            post.getMedia().stream().map(this::toDto).toList(),
            post.getChannelAssignments().stream().map(this::toDto).toList(),
            post.getStatus(),
            post.getType(),
            post.getScheduleInfo().scheduledFor(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }

    /**
     * Convert Media VO to MediaDto
     */
    public MediaDto toDto(Media media) {
        return new MediaDto(
            media.id().toString(),
            media.publicUrl(),
            media.fileName(),
            media.type(),
            media.fileSize(),
            media.width(),
            media.height(),
            media.durationSeconds()
        );
    }

    /**
     * Convert ChannelAssignment VO to ChannelAssignmentDto
     */
    public ChannelAssignmentDto toDto(ChannelAssignment assignment) {
        // Fetch channel details for display
        Channel channel = channelRepository.findById(assignment.channelId())
            .orElse(null);

        String channelName = channel != null ? channel.getProfile().accountName() : "Unknown";
        var platform = channel != null ? channel.getProfile().platform() : null;

        return new ChannelAssignmentDto(
            assignment.channelId().toString(),
            channelName,
            platform,
            assignment.status(),
            assignment.externalPostId(),
            assignment.publishedAt(),
            assignment.errorMessage()
        );
    }

    /**
     * Convert Channel domain model to ChannelDto
     */
    public ChannelDto toDto(Channel channel) {
        return new ChannelDto(
            channel.getId().toString(),
            channel.getProfile().platform(),
            channel.getProfile().accountType(),
            channel.getProfile().accountName(),
            channel.getProfile().accountHandle(),
            channel.getProfile().avatarUrl(),
            channel.isActive(),
            channel.getCreatedAt()
        );
    }

    /**
     * Convert Media VO to UploadMediaResponse
     */
    public UploadMediaResponse toUploadResponse(Media media) {
        return new UploadMediaResponse(
            media.id().toString(),
            media.publicUrl(),
            media.fileName(),
            media.type().name(),
            media.fileSize()
        );
    }
}
