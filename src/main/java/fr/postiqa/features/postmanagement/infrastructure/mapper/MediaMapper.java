package fr.postiqa.features.postmanagement.infrastructure.mapper;

import fr.postiqa.database.entity.MediaEntity;
import fr.postiqa.features.postmanagement.domain.vo.Media;
import fr.postiqa.features.postmanagement.domain.vo.MediaId;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between MediaEntity (JPA) and Media (domain VO).
 */
@Component
public class MediaMapper {

    /**
     * Convert JPA entity to domain value object
     */
    public Media toDomain(MediaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Media.create(
            MediaId.of(entity.getId()),
            entity.getStorageKey(),
            entity.getPublicUrl(),
            entity.getFileName(),
            entity.getMimeType(),
            entity.getType(),
            entity.getFileSize(),
            entity.getWidth(),
            entity.getHeight(),
            entity.getDurationSeconds()
        );
    }

    /**
     * Convert domain value object to JPA entity
     * Note: Post must be set by the caller
     */
    public MediaEntity toEntity(Media media) {
        if (media == null) {
            return null;
        }

        return MediaEntity.builder()
            .id(media.id().value())
            .storageKey(media.storageKey())
            .publicUrl(media.publicUrl())
            .fileName(media.fileName())
            .mimeType(media.mimeType())
            .type(media.type())
            .fileSize(media.fileSize())
            .width(media.width())
            .height(media.height())
            .durationSeconds(media.durationSeconds())
            .build();
    }
}
