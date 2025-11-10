package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Use case for listing social media accounts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ListSocialAccountsUseCase {

    private final SocialAccountPort socialAccountPort;

    public List<SocialAccount> executeForOrganization(UUID organizationId, boolean activeOnly) {
        log.info("Listing social accounts for organization: {}", organizationId);

        if (activeOnly) {
            return socialAccountPort.findActiveByOrganizationId(organizationId);
        }
        return socialAccountPort.findByOrganizationId(organizationId);
    }

    public List<SocialAccount> executeForClient(UUID clientId, boolean activeOnly) {
        log.info("Listing social accounts for client: {}", clientId);

        if (activeOnly) {
            return socialAccountPort.findActiveByClientId(clientId);
        }
        return socialAccountPort.findByClientId(clientId);
    }
}
