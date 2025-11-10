package fr.postiqa.features.postmanagement.infrastructure.mapper;

import fr.postiqa.database.entity.SocialAccountEntity;
import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.vo.*;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between SocialAccountEntity (JPA) and Channel (domain model).
 */
@Component
public class ChannelMapper {

    /**
     * Convert JPA entity to domain model
     */
    public Channel toDomain(SocialAccountEntity entity) {
        if (entity == null) {
            return null;
        }

        // Map channel profile
        ChannelProfile profile = ChannelProfile.create(
            entity.getPlatform(),
            entity.getAccountType(),
            entity.getAccountName(),
            entity.getAccountHandle(),
            null, // profileUrl - not in current entity
            entity.getAccountAvatarUrl()
        );

        // Map client ID (nullable for business)
        ClientId clientId = entity.getClient() != null
            ? ClientId.of(entity.getClient().getId())
            : null;

        // Reconstitute channel
        return Channel.reconstitute(
            ChannelId.of(entity.getId()),
            OrganizationId.of(entity.getOrganization().getId()),
            clientId,
            profile,
            entity.getActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convert domain model to JPA entity
     */
    public SocialAccountEntity toEntity(Channel channel) {
        if (channel == null) {
            return null;
        }

        return SocialAccountEntity.builder()
            .id(channel.getId().value())
            .platform(channel.getProfile().platform())
            .accountType(channel.getProfile().accountType())
            .accountName(channel.getProfile().accountName())
            .accountHandle(channel.getProfile().accountHandle())
            .accountAvatarUrl(channel.getProfile().avatarUrl())
            .active(channel.isActive())
            .build();

        // Note: organization, client, and auth tokens are set by the adapter
    }

    /**
     * Update existing entity from domain model
     */
    public void updateEntity(SocialAccountEntity entity, Channel channel) {
        entity.setAccountName(channel.getProfile().accountName());
        entity.setAccountHandle(channel.getProfile().accountHandle());
        entity.setAccountAvatarUrl(channel.getProfile().avatarUrl());
        entity.setActive(channel.isActive());
    }
}
