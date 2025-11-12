package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for disconnecting a social media account.
 */
@UseCase(
    value = "DisconnectSocialAccount",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Disconnects a social media account"
)
@RequiredArgsConstructor
@Slf4j
public class DisconnectSocialAccountUseCase implements fr.postiqa.shared.usecase.UseCase<DisconnectSocialAccountUseCase.DisconnectCommand, Void> {

    private final SocialAccountPort socialAccountPort;
    private final OAuth2Port oauth2Port;

    /**
     * Command for disconnecting social account
     */
    public record DisconnectCommand(UUID accountId, UUID organizationId) {}

    @Transactional
    public Void execute(DisconnectCommand command) {
        log.info("Disconnecting social account: {}", command.accountId());

        SocialAccount account = socialAccountPort.findByIdAndOrganizationId(command.accountId(), command.organizationId())
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + command.accountId()));

        try {
            // Revoke OAuth2 token
            oauth2Port.revokeToken(account.getPlatform(), account.getToken().getAccessToken());
        } catch (Exception e) {
            log.warn("Failed to revoke token for account: {}, continuing with deletion", command.accountId(), e);
        }

        // Delete account
        socialAccountPort.delete(command.accountId());

        log.info("Successfully disconnected social account: {}", command.accountId());

        return null;
    }
}
