package fr.postiqa.features.postmanagement.adapter.out.persistence;

import fr.postiqa.database.entity.SocialAccountEntity;
import fr.postiqa.database.repository.SocialAccountRepository;
import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.port.ChannelRepositoryPort;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.domain.vo.ClientId;
import fr.postiqa.features.postmanagement.domain.vo.OrganizationId;
import fr.postiqa.features.postmanagement.infrastructure.mapper.ChannelMapper;
import fr.postiqa.shared.enums.SocialPlatform;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing ChannelRepositoryPort.
 * Bridges between domain Channel model and SocialAccountEntity.
 */
@Repository
public class ChannelJpaRepositoryAdapter implements ChannelRepositoryPort {

    private final SocialAccountRepository socialAccountRepository;
    private final ChannelMapper channelMapper;

    public ChannelJpaRepositoryAdapter(
        SocialAccountRepository socialAccountRepository,
        ChannelMapper channelMapper
    ) {
        this.socialAccountRepository = socialAccountRepository;
        this.channelMapper = channelMapper;
    }

    @Override
    public Channel save(Channel channel) {
        Optional<SocialAccountEntity> existingEntity = socialAccountRepository.findById(channel.getId().value());

        SocialAccountEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing
            entity = existingEntity.get();
            channelMapper.updateEntity(entity, channel);
        } else {
            // Create new (not implemented - channels are created via OAuth flow)
            throw new UnsupportedOperationException("Channel creation is handled by OAuth flow");
        }

        SocialAccountEntity saved = socialAccountRepository.save(entity);
        return channelMapper.toDomain(saved);
    }

    @Override
    public Optional<Channel> findById(ChannelId channelId) {
        return socialAccountRepository.findById(channelId.value())
            .map(channelMapper::toDomain);
    }

    @Override
    public Optional<Channel> findByIdAndOrganization(ChannelId channelId, OrganizationId organizationId) {
        return socialAccountRepository.findByIdAndOrganizationId(channelId.value(), organizationId.value())
            .map(channelMapper::toDomain);
    }

    @Override
    public Optional<Channel> findByIdAndClient(ChannelId channelId, ClientId clientId) {
        return socialAccountRepository.findByIdAndClientId(channelId.value(), clientId.value())
            .map(channelMapper::toDomain);
    }

    @Override
    public List<Channel> findByOrganization(OrganizationId organizationId) {
        return socialAccountRepository.findByOrganizationId(organizationId.value())
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public List<Channel> findActiveByOrganization(OrganizationId organizationId) {
        return socialAccountRepository.findByOrganizationIdAndActiveTrue(organizationId.value())
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public List<Channel> findByClient(ClientId clientId) {
        return socialAccountRepository.findByClientId(clientId.value())
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public List<Channel> findActiveByClient(ClientId clientId) {
        return socialAccountRepository.findByClientIdAndActiveTrue(clientId.value())
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public List<Channel> findByOrganizationAndPlatform(OrganizationId organizationId, SocialPlatform platform) {
        return socialAccountRepository.findByOrganizationIdAndPlatform(organizationId.value(), platform)
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public List<Channel> findByClientAndPlatform(ClientId clientId, SocialPlatform platform) {
        return socialAccountRepository.findByClientIdAndPlatform(clientId.value(), platform)
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public List<Channel> findByIds(List<ChannelId> channelIds) {
        List<UUID> uuids = channelIds.stream().map(ChannelId::value).toList();
        return socialAccountRepository.findAllById(uuids)
            .stream()
            .map(channelMapper::toDomain)
            .toList();
    }

    @Override
    public void delete(ChannelId channelId) {
        socialAccountRepository.deleteById(channelId.value());
    }

    @Override
    public boolean existsByIdAndOrganization(ChannelId channelId, OrganizationId organizationId) {
        return socialAccountRepository.findByIdAndOrganizationId(channelId.value(), organizationId.value())
            .isPresent();
    }

    @Override
    public boolean existsByIdAndClient(ChannelId channelId, ClientId clientId) {
        return socialAccountRepository.findByIdAndClientId(channelId.value(), clientId.value())
            .isPresent();
    }
}
