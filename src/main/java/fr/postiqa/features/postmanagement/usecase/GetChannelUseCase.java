package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.ChannelNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.port.ChannelRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a single channel by ID.
 */
@Component
public class GetChannelUseCase {

    private final ChannelRepositoryPort channelRepository;
    private final TenantAccessPort tenantAccess;

    public GetChannelUseCase(
        ChannelRepositoryPort channelRepository,
        TenantAccessPort tenantAccess
    ) {
        this.channelRepository = channelRepository;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Query for getting a channel
     */
    public record GetChannelQuery(ChannelId channelId) {}

    /**
     * Execute the get channel use case
     */
    @Transactional(readOnly = true)
    public Channel execute(GetChannelQuery query) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find channel with authorization check
        Channel channel;

        if (tenant.isAgency()) {
            channel = channelRepository.findByIdAndClient(query.channelId(), tenant.clientId())
                .orElseThrow(() -> new ChannelNotFoundException(query.channelId()));
        } else {
            channel = channelRepository.findByIdAndOrganization(query.channelId(), tenant.organizationId())
                .orElseThrow(() -> new ChannelNotFoundException(query.channelId()));
        }

        if (!tenantAccess.canAccessChannel(query.channelId())) {
            throw new UnauthorizedAccessException(tenant.userId(), "Channel", query.channelId().toString());
        }

        return channel;
    }
}
