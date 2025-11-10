package fr.postiqa.features.postmanagement.domain.vo;

import java.util.UUID;

/**
 * Value object representing a Post identifier.
 * Provides type safety and prevents mixing different ID types.
 */
public record PostId(UUID value) {

    public PostId {
        if (value == null) {
            throw new IllegalArgumentException("PostId cannot be null");
        }
    }

    /**
     * Create a new random PostId
     */
    public static PostId generate() {
        return new PostId(UUID.randomUUID());
    }

    /**
     * Create a PostId from a UUID string
     */
    public static PostId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("PostId string cannot be null or blank");
        }
        try {
            return new PostId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PostId format: " + id, e);
        }
    }

    /**
     * Create a PostId from a UUID
     */
    public static PostId of(UUID uuid) {
        return new PostId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
