package fr.postiqa.features.postmanagement.domain.vo;

import java.util.UUID;

/**
 * Value object representing an Organization identifier.
 * Provides type safety and prevents mixing different ID types.
 */
public record OrganizationId(UUID value) {

    public OrganizationId {
        if (value == null) {
            throw new IllegalArgumentException("OrganizationId cannot be null");
        }
    }

    /**
     * Create an OrganizationId from a UUID string
     */
    public static OrganizationId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("OrganizationId string cannot be null or blank");
        }
        try {
            return new OrganizationId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid OrganizationId format: " + id, e);
        }
    }

    /**
     * Create an OrganizationId from a UUID
     */
    public static OrganizationId of(UUID uuid) {
        return new OrganizationId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
