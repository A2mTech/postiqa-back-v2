package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.exception.socialaccount.OAuth2TokenRefreshException;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for refreshing an expired OAuth2 token.
 */
@UseCase(
    value = "RefreshToken",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Refreshes an expired OAuth2 token"
)
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenUseCase implements fr.postiqa.shared.usecase.UseCase<RefreshTokenUseCase.RefreshTokenCommand, SocialAccount> {

    private final SocialAccountPort socialAccountPort;
    private final OAuth2Port oauth2Port;

    /**
     * Command for refreshing token
     */
    public record RefreshTokenCommand(UUID accountId) {}

    @Transactional
    public SocialAccount execute(RefreshTokenCommand command) {
        log.info("Refreshing token for social account: {}", command.accountId());

        SocialAccount account = socialAccountPort.findById(command.accountId())
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + command.accountId()));

        if (!account.getToken().hasRefreshToken()) {
            throw new OAuth2TokenRefreshException("No refresh token available for account: " + command.accountId());
        }

        try {
            OAuth2Token newToken = oauth2Port.refreshToken(
                account.getPlatform(),
                account.getToken().getRefreshToken()
            );

            account.updateToken(newToken);
            SocialAccount updatedAccount = socialAccountPort.save(account);

            log.info("Successfully refreshed token for social account: {}", command.accountId());

            return updatedAccount;

        } catch (Exception e) {
            log.error("Failed to refresh token for account: {}", command.accountId(), e);
            throw new OAuth2TokenRefreshException("Failed to refresh token: " + e.getMessage(), e);
        }
    }
}
