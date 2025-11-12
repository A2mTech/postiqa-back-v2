package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.ConnectionTestResult;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.features.socialaccounts.domain.port.SocialPlatformApiPort;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Use case for testing a social account connection.
 */
@UseCase(
    value = "TestConnection",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Tests a social account connection",
    logActivity = false
)
@RequiredArgsConstructor
@Slf4j
public class TestConnectionUseCase implements fr.postiqa.shared.usecase.UseCase<TestConnectionUseCase.TestConnectionCommand, ConnectionTestResult> {

    private final SocialAccountPort socialAccountPort;
    private final SocialPlatformApiPort platformApiPort;

    /**
     * Command for testing connection
     */
    public record TestConnectionCommand(UUID accountId, UUID organizationId) {}

    public ConnectionTestResult execute(TestConnectionCommand command) {
        log.info("Testing connection for social account: {}", command.accountId());

        SocialAccount account = socialAccountPort.findByIdAndOrganizationId(command.accountId(), command.organizationId())
            .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + command.accountId()));

        if (account.isTokenExpired()) {
            return ConnectionTestResult.failure(
                "Token is expired",
                "Please refresh the token or reconnect the account"
            );
        }

        return platformApiPort.testConnection(account.getPlatform(), account.getToken().getAccessToken());
    }
}
