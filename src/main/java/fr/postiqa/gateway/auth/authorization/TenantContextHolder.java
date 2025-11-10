package fr.postiqa.gateway.auth.authorization;

import java.util.UUID;

/**
 * ThreadLocal holder for tenant context.
 * Stores current user, organization and client IDs for horizontal scope validation.
 */
public class TenantContextHolder {

    private static final ThreadLocal<UUID> userId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> organizationId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> clientId = new ThreadLocal<>();

    /**
     * Set user ID for current thread
     */
    public static void setUserId(UUID usrId) {
        userId.set(usrId);
    }

    /**
     * Get user ID from current thread
     */
    public static UUID getUserId() {
        return userId.get();
    }

    /**
     * Set organization ID for current thread
     */
    public static void setOrganizationId(UUID orgId) {
        organizationId.set(orgId);
    }

    /**
     * Get organization ID from current thread
     */
    public static UUID getOrganizationId() {
        return organizationId.get();
    }

    /**
     * Set client ID for current thread (agency mode)
     */
    public static void setClientId(UUID cltId) {
        clientId.set(cltId);
    }

    /**
     * Get client ID from current thread (agency mode)
     */
    public static UUID getClientId() {
        return clientId.get();
    }

    /**
     * Clear tenant context for current thread
     */
    public static void clear() {
        userId.remove();
        organizationId.remove();
        clientId.remove();
    }
}
