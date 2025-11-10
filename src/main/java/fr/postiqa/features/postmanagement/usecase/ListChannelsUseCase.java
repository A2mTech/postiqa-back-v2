package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.port.ChannelRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.shared.enums.SocialPlatform;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for listing channels with optional filters.
 */
@Component
public class ListChannelsUseCase {

    private final ChannelRepositoryPort channelRepository;
    private final TenantAccessPort tenantAccess;

    public ListChannelsUseCase(
        ChannelRepositoryPort channelRepository,
        TenantAccessPort tenantAccess
    ) {
        this.channelRepository = channelRepository;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Query for listing channels
     */
    public record ListChannelsQuery(
        SocialPlatform platform,
        boolean activeOnly
    ) {}

    /**
     * Execute the list channels use case
     */
    @Transactional(readOnly = true)
    public List<Channel> execute(ListChannelsQuery query) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // List channels based on tenant type and filters
        if (query.platform() != null) {
            if (tenant.isAgency()) {
                return channelRepository.findByClientAndPlatform(tenant.clientId(), query.platform());
            } else {
                return channelRepository.findByOrganizationAndPlatform(tenant.organizationId(), query.platform());
            }
        } else if (query.activeOnly()) {
            if (tenant.isAgency()) {
                return channelRepository.findActiveByClient(tenant.clientId());
            } else {
                return channelRepository.findActiveByOrganization(tenant.organizationId());
            }
        } else {
            if (tenant.isAgency()) {
                return channelRepository.findByClient(tenant.clientId());
            } else {
                return channelRepository.findByOrganization(tenant.organizationId());
            }
        }
    }
}
