package fr.postiqa.features.postmanagement.domain.port;

import fr.postiqa.features.postmanagement.domain.vo.*;

/**
 * Port for tenant access control and authorization.
 * Implemented by security adapters.
 */
public interface TenantAccessPort {

    /**
     * Tenant context information
     */
    record TenantContext(
        UserId userId,
        OrganizationId organizationId,
        ClientId clientId,
        boolean isAgency
    ) {
        public TenantContext {
            if (userId == null) {
                throw new IllegalArgumentException("UserId cannot be null");
            }
            if (organizationId == null) {
                throw new IllegalArgumentException("OrganizationId cannot be null");
            }
            if (isAgency && clientId == null) {
                throw new IllegalArgumentException("ClientId cannot be null for agency context");
            }
        }

        /**
         * Create business context (no client)
         */
        public static TenantContext business(UserId userId, OrganizationId organizationId) {
            return new TenantContext(userId, organizationId, null, false);
        }

        /**
         * Create agency context (with client)
         */
        public static TenantContext agency(UserId userId, OrganizationId organizationId, ClientId clientId) {
            return new TenantContext(userId, organizationId, clientId, true);
        }
    }

    /**
     * Get current tenant context from security context
     */
    TenantContext getCurrentTenant();

    /**
     * Check if user can access a specific channel
     */
    boolean canAccessChannel(ChannelId channelId);

    /**
     * Check if user can access a specific post
     */
    boolean canAccessPost(PostId postId);

    /**
     * Check if user can access an organization
     */
    boolean canAccessOrganization(OrganizationId organizationId);

    /**
     * Check if user can access a client
     */
    boolean canAccessClient(ClientId clientId);
}
