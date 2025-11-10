package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for disconnecting a social media account.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DisconnectSocialAccountUseCase {

    private final SocialAccountPort socialAccountPort;
    private final OAuth2Port oauth2Port;

    @Transactional
    public void execute(UUID accountId, UUID organizationId) {
        log.info("Disconnecting social account: {}", accountId);

        SocialAccount account = socialAccountPort.findByIdAndOrganizationId(accountId, organizationId)
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + accountId));

        try {
            // Revoke OAuth2 token
            oauth2Port.revokeToken(account.getPlatform(), account.getToken().getAccessToken());
        } catch (Exception e) {
            log.warn("Failed to revoke token for account: {}, continuing with deletion", accountId, e);
        }

        // Delete account
        socialAccountPort.delete(accountId);

        log.info("Successfully disconnected social account: {}", accountId);
    }
}
