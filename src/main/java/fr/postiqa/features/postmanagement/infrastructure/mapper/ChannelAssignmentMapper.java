package fr.postiqa.features.postmanagement.infrastructure.mapper;

import fr.postiqa.database.entity.PostChannelEntity;
import fr.postiqa.features.postmanagement.domain.vo.ChannelAssignment;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between PostChannelEntity (JPA) and ChannelAssignment (domain VO).
 */
@Component
public class ChannelAssignmentMapper {

    /**
     * Convert JPA entity to domain value object
     */
    public ChannelAssignment toDomain(PostChannelEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ChannelAssignment(
            ChannelId.of(entity.getChannel().getId()),
            entity.getStatus(),
            entity.getExternalPostId(),
            entity.getPublishedAt(),
            entity.getErrorMessage()
        );
    }

    /**
     * Convert domain value object to JPA entity
     * Note: Post and Channel must be set by the caller
     */
    public PostChannelEntity toEntity(ChannelAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        return PostChannelEntity.builder()
            .status(assignment.status())
            .externalPostId(assignment.externalPostId())
            .publishedAt(assignment.publishedAt())
            .errorMessage(assignment.errorMessage())
            .build();
    }

    /**
     * Update existing entity from domain value object
     */
    public void updateEntity(PostChannelEntity entity, ChannelAssignment assignment) {
        entity.setStatus(assignment.status());
        entity.setExternalPostId(assignment.externalPostId());
        entity.setPublishedAt(assignment.publishedAt());
        entity.setErrorMessage(assignment.errorMessage());
    }
}
