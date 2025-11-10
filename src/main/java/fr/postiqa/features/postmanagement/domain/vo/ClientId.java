package fr.postiqa.features.postmanagement.domain.vo;

import java.util.UUID;

/**
 * Value object representing a Client identifier (for agency multi-tenant).
 * Provides type safety and prevents mixing different ID types.
 */
public record ClientId(UUID value) {

    public ClientId {
        if (value == null) {
            throw new IllegalArgumentException("ClientId cannot be null");
        }
    }

    /**
     * Create a ClientId from a UUID string
     */
    public static ClientId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ClientId string cannot be null or blank");
        }
        try {
            return new ClientId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ClientId format: " + id, e);
        }
    }

    /**
     * Create a ClientId from a UUID
     */
    public static ClientId of(UUID uuid) {
        return new ClientId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
