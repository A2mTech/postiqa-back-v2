package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.exception.socialaccount.OAuth2TokenRefreshException;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for refreshing an expired OAuth2 token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenUseCase {

    private final SocialAccountPort socialAccountPort;
    private final OAuth2Port oauth2Port;

    @Transactional
    public SocialAccount execute(UUID accountId) {
        log.info("Refreshing token for social account: {}", accountId);

        SocialAccount account = socialAccountPort.findById(accountId)
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + accountId));

        if (!account.getToken().hasRefreshToken()) {
            throw new OAuth2TokenRefreshException("No refresh token available for account: " + accountId);
        }

        try {
            OAuth2Token newToken = oauth2Port.refreshToken(
                account.getPlatform(),
                account.getToken().getRefreshToken()
            );

            account.updateToken(newToken);
            SocialAccount updatedAccount = socialAccountPort.save(account);

            log.info("Successfully refreshed token for social account: {}", accountId);

            return updatedAccount;

        } catch (Exception e) {
            log.error("Failed to refresh token for account: {}", accountId, e);
            throw new OAuth2TokenRefreshException("Failed to refresh token: " + e.getMessage(), e);
        }
    }
}
