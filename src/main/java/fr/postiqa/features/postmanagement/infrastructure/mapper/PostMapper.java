package fr.postiqa.features.postmanagement.infrastructure.mapper;

import fr.postiqa.database.entity.PostChannelEntity;
import fr.postiqa.database.entity.PostEntity;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.vo.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting between PostEntity (JPA) and Post (domain model).
 */
@Component
public class PostMapper {

    private final MediaMapper mediaMapper;
    private final ChannelAssignmentMapper channelAssignmentMapper;

    public PostMapper(MediaMapper mediaMapper, ChannelAssignmentMapper channelAssignmentMapper) {
        this.mediaMapper = mediaMapper;
        this.channelAssignmentMapper = channelAssignmentMapper;
    }

    /**
     * Convert JPA entity to domain model
     */
    public Post toDomain(PostEntity entity) {
        if (entity == null) {
            return null;
        }

        // Map content
        Content content = mapContentFromEntity(entity);

        // Map media
        List<Media> media = entity.getMedia().stream()
            .map(mediaMapper::toDomain)
            .toList();

        // Map channel assignments
        List<ChannelAssignment> channelAssignments = entity.getChannels().stream()
            .map(channelAssignmentMapper::toDomain)
            .toList();

        // Map schedule info
        ScheduleInfo scheduleInfo = entity.getScheduledFor() != null
            ? ScheduleInfo.scheduledFor(entity.getScheduledFor())
            : ScheduleInfo.immediate();

        // Reconstitute post
        return Post.reconstitute(
            PostId.of(entity.getId()),
            UserId.of(entity.getCreatedBy().getId()),
            content,
            media,
            entity.getStatus(),
            entity.getType(),
            scheduleInfo,
            channelAssignments,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convert domain model to JPA entity
     */
    public PostEntity toEntity(Post post) {
        if (post == null) {
            return null;
        }

        PostEntity entity = PostEntity.builder()
            .id(post.getId().value())
            .content(post.getContent().text())
            .status(post.getStatus())
            .type(post.getType())
            .scheduledFor(post.getScheduleInfo().scheduledFor())
            .metadata(mapContentToMetadata(post.getContent()))
            .build();

        // Note: createdBy, channels, and media are set by the adapter
        // because they require fetching related entities

        return entity;
    }

    /**
     * Update existing entity from domain model
     */
    public void updateEntity(PostEntity entity, Post post) {
        entity.setContent(post.getContent().text());
        entity.setStatus(post.getStatus());
        entity.setScheduledFor(post.getScheduleInfo().scheduledFor());
        entity.setMetadata(mapContentToMetadata(post.getContent()));
    }

    private Content mapContentFromEntity(PostEntity entity) {
        String text = entity.getContent();

        // Extract hashtags and mentions from metadata if available
        List<String> hashtags = List.of();
        List<String> mentions = List.of();

        if (entity.getMetadata() != null) {
            Object hashtagsObj = entity.getMetadata().get("hashtags");
            Object mentionsObj = entity.getMetadata().get("mentions");

            if (hashtagsObj instanceof List<?> list) {
                hashtags = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            }

            if (mentionsObj instanceof List<?> list) {
                mentions = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            }
        }

        return Content.create(text, hashtags, mentions);
    }

    private Map<String, Object> mapContentToMetadata(Content content) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hashtags", content.hashtags());
        metadata.put("mentions", content.mentions());
        return metadata;
    }
}
