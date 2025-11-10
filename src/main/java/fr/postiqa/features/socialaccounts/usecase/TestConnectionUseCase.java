package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.ConnectionTestResult;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.features.socialaccounts.domain.port.SocialPlatformApiPort;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Use case for testing a social account connection.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestConnectionUseCase {

    private final SocialAccountPort socialAccountPort;
    private final SocialPlatformApiPort platformApiPort;

    public ConnectionTestResult execute(UUID accountId, UUID organizationId) {
        log.info("Testing connection for social account: {}", accountId);

        SocialAccount account = socialAccountPort.findByIdAndOrganizationId(accountId, organizationId)
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + accountId));

        if (account.isTokenExpired()) {
            return ConnectionTestResult.failure(
                "Token is expired",
                "Please refresh the token or reconnect the account"
            );
        }

        return platformApiPort.testConnection(account.getPlatform(), account.getToken().getAccessToken());
    }
}
