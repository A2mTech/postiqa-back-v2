package fr.postiqa.features.postmanagement.domain.vo;

import java.util.UUID;

/**
 * Value object representing a Media identifier.
 * Provides type safety and prevents mixing different ID types.
 */
public record MediaId(UUID value) {

    public MediaId {
        if (value == null) {
            throw new IllegalArgumentException("MediaId cannot be null");
        }
    }

    /**
     * Create a new random MediaId
     */
    public static MediaId generate() {
        return new MediaId(UUID.randomUUID());
    }

    /**
     * Create a MediaId from a UUID string
     */
    public static MediaId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("MediaId string cannot be null or blank");
        }
        try {
            return new MediaId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid MediaId format: " + id, e);
        }
    }

    /**
     * Create a MediaId from a UUID
     */
    public static MediaId of(UUID uuid) {
        return new MediaId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
