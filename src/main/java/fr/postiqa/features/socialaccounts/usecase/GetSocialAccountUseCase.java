package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.exception.socialaccount.SocialAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Use case for getting a social account by ID.
 */
@UseCase(
    value = "GetSocialAccount",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Gets a social account by ID",
    logActivity = false
)
@RequiredArgsConstructor
@Slf4j
public class GetSocialAccountUseCase implements fr.postiqa.shared.usecase.UseCase<GetSocialAccountUseCase.GetAccountCommand, SocialAccount> {

    private final SocialAccountPort socialAccountPort;

    /**
     * Command for getting social account
     */
    public record GetAccountCommand(
        UUID accountId,
        UUID organizationId,
        UUID clientId
    ) {}

    public SocialAccount execute(GetAccountCommand command) {
        if (command.clientId() != null) {
            log.info("Getting social account: {} for client: {}", command.accountId(), command.clientId());
            return socialAccountPort.findByIdAndClientId(command.accountId(), command.clientId())
                .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + command.accountId()));
        } else {
            log.info("Getting social account: {}", command.accountId());
            return socialAccountPort.findByIdAndOrganizationId(command.accountId(), command.organizationId())
                .orElseThrow(() -> new SocialAccountNotFoundException("Social account not found: " + command.accountId()));
        }
    }
}
