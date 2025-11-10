package fr.postiqa.features.postmanagement.domain.port;

import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.domain.vo.ClientId;
import fr.postiqa.features.postmanagement.domain.vo.OrganizationId;
import fr.postiqa.shared.enums.SocialPlatform;

import java.util.List;
import java.util.Optional;

/**
 * Port for Channel persistence operations.
 * Implemented by persistence adapters.
 */
public interface ChannelRepositoryPort {

    /**
     * Save a channel (create or update)
     */
    Channel save(Channel channel);

    /**
     * Find channel by ID
     */
    Optional<Channel> findById(ChannelId channelId);

    /**
     * Find channel by ID with authorization check for organization
     */
    Optional<Channel> findByIdAndOrganization(ChannelId channelId, OrganizationId organizationId);

    /**
     * Find channel by ID for a specific client (agency)
     */
    Optional<Channel> findByIdAndClient(ChannelId channelId, ClientId clientId);

    /**
     * Find all channels for an organization
     */
    List<Channel> findByOrganization(OrganizationId organizationId);

    /**
     * Find all active channels for an organization
     */
    List<Channel> findActiveByOrganization(OrganizationId organizationId);

    /**
     * Find all channels for a client
     */
    List<Channel> findByClient(ClientId clientId);

    /**
     * Find all active channels for a client
     */
    List<Channel> findActiveByClient(ClientId clientId);

    /**
     * Find channels by organization and platform
     */
    List<Channel> findByOrganizationAndPlatform(OrganizationId organizationId, SocialPlatform platform);

    /**
     * Find channels by client and platform
     */
    List<Channel> findByClientAndPlatform(ClientId clientId, SocialPlatform platform);

    /**
     * Find channels by IDs (for batch operations)
     */
    List<Channel> findByIds(List<ChannelId> channelIds);

    /**
     * Delete a channel
     */
    void delete(ChannelId channelId);

    /**
     * Check if channel exists and belongs to organization
     */
    boolean existsByIdAndOrganization(ChannelId channelId, OrganizationId organizationId);

    /**
     * Check if channel exists and belongs to client
     */
    boolean existsByIdAndClient(ChannelId channelId, ClientId clientId);
}
