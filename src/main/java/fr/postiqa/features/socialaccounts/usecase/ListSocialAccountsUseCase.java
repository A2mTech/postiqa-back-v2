package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Use case for listing social media accounts.
 */
@UseCase(
    value = "ListSocialAccounts",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Lists social media accounts",
    logActivity = false
)
@RequiredArgsConstructor
@Slf4j
public class ListSocialAccountsUseCase implements fr.postiqa.shared.usecase.UseCase<ListSocialAccountsUseCase.ListAccountsCommand, List<SocialAccount>> {

    private final SocialAccountPort socialAccountPort;

    /**
     * Command for listing social accounts
     */
    public record ListAccountsCommand(
        UUID organizationId,
        UUID clientId,
        boolean activeOnly
    ) {}

    public List<SocialAccount> execute(ListAccountsCommand command) {
        if (command.clientId() != null) {
            log.info("Listing social accounts for client: {}", command.clientId());
            if (command.activeOnly()) {
                return socialAccountPort.findActiveByClientId(command.clientId());
            }
            return socialAccountPort.findByClientId(command.clientId());
        } else {
            log.info("Listing social accounts for organization: {}", command.organizationId());
            if (command.activeOnly()) {
                return socialAccountPort.findActiveByOrganizationId(command.organizationId());
            }
            return socialAccountPort.findByOrganizationId(command.organizationId());
        }
    }
}
