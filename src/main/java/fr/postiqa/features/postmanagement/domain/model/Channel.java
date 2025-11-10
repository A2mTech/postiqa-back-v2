package fr.postiqa.features.postmanagement.domain.model;

import fr.postiqa.features.postmanagement.domain.vo.*;

import java.time.Instant;

/**
 * Channel aggregate root representing a social media account/channel.
 * Contains business logic for channel management and authorization.
 */
public class Channel {
    private final ChannelId id;
    private final OrganizationId organizationId;
    private final ClientId clientId; // Nullable for business organizations
    private ChannelProfile profile;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    // Private constructor for domain control
    private Channel(
        ChannelId id,
        OrganizationId organizationId,
        ClientId clientId,
        ChannelProfile profile,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.clientId = clientId;
        this.profile = profile;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Create a new channel for a business organization
     */
    public static Channel createForBusiness(OrganizationId organizationId, ChannelProfile profile) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Channel profile cannot be null");
        }

        ChannelId channelId = ChannelId.generate();
        Instant now = Instant.now();
        return new Channel(channelId, organizationId, null, profile, true, now, now);
    }

    /**
     * Create a new channel for an agency client
     */
    public static Channel createForClient(OrganizationId organizationId, ClientId clientId, ChannelProfile profile) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null for agency channels");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Channel profile cannot be null");
        }

        ChannelId channelId = ChannelId.generate();
        Instant now = Instant.now();
        return new Channel(channelId, organizationId, clientId, profile, true, now, now);
    }

    /**
     * Reconstitute a channel from persistence (for use by repository)
     */
    public static Channel reconstitute(
        ChannelId id,
        OrganizationId organizationId,
        ClientId clientId,
        ChannelProfile profile,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Channel(id, organizationId, clientId, profile, active, createdAt, updatedAt);
    }

    /**
     * Update channel profile
     */
    public void updateProfile(ChannelProfile newProfile) {
        if (newProfile == null) {
            throw new IllegalArgumentException("Channel profile cannot be null");
        }
        if (!this.profile.platform().equals(newProfile.platform())) {
            throw new IllegalArgumentException("Cannot change platform of existing channel");
        }
        this.profile = newProfile;
        this.updatedAt = Instant.now();
    }

    /**
     * Deactivate channel
     */
    public void deactivate() {
        if (!active) {
            throw new IllegalStateException("Channel is already deactivated");
        }
        this.active = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Activate channel
     */
    public void activate() {
        if (active) {
            throw new IllegalStateException("Channel is already active");
        }
        this.active = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if channel belongs to a specific organization (and optionally client)
     */
    public boolean belongsTo(OrganizationId orgId, ClientId cliId) {
        if (orgId == null) {
            return false;
        }
        boolean orgMatches = this.organizationId.equals(orgId);

        // If clientId is provided, it must match
        if (cliId != null) {
            return orgMatches && cliId.equals(this.clientId);
        }

        // If no clientId provided, channel should not belong to a client (business channel)
        return orgMatches && this.clientId == null;
    }

    /**
     * Check if channel belongs to an agency client
     */
    public boolean belongsToClient(ClientId cliId) {
        return cliId != null && cliId.equals(this.clientId);
    }

    /**
     * Check if channel is a business channel (not client)
     */
    public boolean isBusinessChannel() {
        return clientId == null;
    }

    /**
     * Check if channel is a client channel (agency)
     */
    public boolean isClientChannel() {
        return clientId != null;
    }

    // Getters

    public ChannelId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public ChannelProfile getProfile() {
        return profile;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return id.equals(channel.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Channel{" +
               "id=" + id +
               ", organizationId=" + organizationId +
               ", clientId=" + clientId +
               ", profile=" + profile +
               ", active=" + active +
               '}';
    }
}
