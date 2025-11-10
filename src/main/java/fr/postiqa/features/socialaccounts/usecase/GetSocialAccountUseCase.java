package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Use case for getting a social account by ID.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetSocialAccountUseCase {

    private final SocialAccountPort socialAccountPort;

    public SocialAccount execute(UUID accountId, UUID organizationId) {
        log.info("Getting social account: {}", accountId);

        return socialAccountPort.findByIdAndOrganizationId(accountId, organizationId)
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + accountId));
    }

    public SocialAccount executeForClient(UUID accountId, UUID clientId) {
        log.info("Getting social account: {} for client: {}", accountId, clientId);

        return socialAccountPort.findByIdAndClientId(accountId, clientId)
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + accountId));
    }
}
