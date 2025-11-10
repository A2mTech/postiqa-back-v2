package fr.postiqa.features.postmanagement.domain.vo;

import java.util.UUID;

/**
 * Value object representing a User identifier.
 * Provides type safety and prevents mixing different ID types.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    /**
     * Create a UserId from a UUID string
     */
    public static UserId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("UserId string cannot be null or blank");
        }
        try {
            return new UserId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UserId format: " + id, e);
        }
    }

    /**
     * Create a UserId from a UUID
     */
    public static UserId of(UUID uuid) {
        return new UserId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
