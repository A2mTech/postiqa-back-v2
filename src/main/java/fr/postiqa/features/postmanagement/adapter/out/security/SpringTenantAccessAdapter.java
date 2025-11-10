package fr.postiqa.features.postmanagement.adapter.out.security;

import fr.postiqa.database.repository.ClientRepository;
import fr.postiqa.database.repository.OrganizationRepository;
import fr.postiqa.database.repository.PostRepository;
import fr.postiqa.database.repository.SocialAccountRepository;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.*;
import fr.postiqa.gateway.auth.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security adapter implementing TenantAccessPort.
 * Provides tenant context and authorization checks based on Spring Security.
 */
@Component
public class SpringTenantAccessAdapter implements TenantAccessPort {

    private final PostRepository postRepository;
    private final SocialAccountRepository socialAccountRepository;

    public SpringTenantAccessAdapter(
        PostRepository postRepository,
        SocialAccountRepository socialAccountRepository
    ) {
        this.postRepository = postRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Override
    public TenantContext getCurrentTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException("Invalid authentication principal");
        }

        UserId userId = UserId.of(userDetails.getId());

        // Extract organization and client from user scopes
        // Assuming first scope contains the tenant context
        var scopes = userDetails.getScopes();
        if (scopes.isEmpty()) {
            throw new IllegalStateException("User has no scopes");
        }

        var primaryScope = scopes.get(0);
        OrganizationId organizationId = OrganizationId.of(primaryScope.getOrganizationId());

        if (primaryScope.getClientId() != null) {
            // Agency context
            ClientId clientId = ClientId.of(primaryScope.getClientId());
            return TenantContext.agency(userId, organizationId, clientId);
        } else {
            // Business context
            return TenantContext.business(userId, organizationId);
        }
    }

    @Override
    public boolean canAccessChannel(ChannelId channelId) {
        TenantContext tenant = getCurrentTenant();

        if (tenant.isAgency()) {
            return socialAccountRepository.findByIdAndClientId(
                channelId.value(),
                tenant.clientId().value()
            ).isPresent();
        } else {
            return socialAccountRepository.findByIdAndOrganizationId(
                channelId.value(),
                tenant.organizationId().value()
            ).isPresent();
        }
    }

    @Override
    public boolean canAccessPost(PostId postId) {
        TenantContext tenant = getCurrentTenant();

        if (tenant.isAgency()) {
            return postRepository.existsByIdAndClientId(
                postId.value(),
                tenant.clientId().value()
            );
        } else {
            return postRepository.existsByIdAndOrganizationId(
                postId.value(),
                tenant.organizationId().value()
            );
        }
    }

    @Override
    public boolean canAccessOrganization(OrganizationId organizationId) {
        TenantContext tenant = getCurrentTenant();
        return tenant.organizationId().equals(organizationId);
    }

    @Override
    public boolean canAccessClient(ClientId clientId) {
        TenantContext tenant = getCurrentTenant();
        return tenant.isAgency() && tenant.clientId().equals(clientId);
    }
}
