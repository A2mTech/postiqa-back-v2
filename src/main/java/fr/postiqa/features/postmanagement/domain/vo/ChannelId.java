package fr.postiqa.features.postmanagement.domain.vo;

import java.util.UUID;

/**
 * Value object representing a Channel (Social Account) identifier.
 * Provides type safety and prevents mixing different ID types.
 */
public record ChannelId(UUID value) {

    public ChannelId {
        if (value == null) {
            throw new IllegalArgumentException("ChannelId cannot be null");
        }
    }

    /**
     * Create a new random ChannelId
     */
    public static ChannelId generate() {
        return new ChannelId(UUID.randomUUID());
    }

    /**
     * Create a ChannelId from a UUID string
     */
    public static ChannelId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ChannelId string cannot be null or blank");
        }
        try {
            return new ChannelId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ChannelId format: " + id, e);
        }
    }

    /**
     * Create a ChannelId from a UUID
     */
    public static ChannelId of(UUID uuid) {
        return new ChannelId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
