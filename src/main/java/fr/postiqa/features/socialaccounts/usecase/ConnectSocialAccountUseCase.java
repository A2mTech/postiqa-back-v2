package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.enums.SocialPlatform;
import fr.postiqa.shared.exception.socialaccount.OAuth2AuthenticationException;
import fr.postiqa.shared.exception.socialaccount.SocialAccountAlreadyConnectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for connecting a social media account.
 * Handles OAuth2 callback and saves the connected account.
 */
@UseCase(
    value = "ConnectSocialAccount",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Connects a social media account"
)
@RequiredArgsConstructor
@Slf4j
public class ConnectSocialAccountUseCase implements fr.postiqa.shared.usecase.UseCase<ConnectSocialAccountUseCase.ConnectAccountCommand, SocialAccount> {

    private final OAuth2Port oauth2Port;
    private final SocialAccountPort socialAccountPort;

    /**
     * Command for connecting social account
     */
    public record ConnectAccountCommand(
        SocialPlatform platform,
        String code,
        String redirectUri,
        UUID userId,
        UUID organizationId,
        UUID clientId,
        String scopes
    ) {}

    /**
     * Execute the use case.
     *
     * @param command Command with connection details
     * @return Connected social account
     */
    @Transactional
    public SocialAccount execute(ConnectAccountCommand command) {
        log.info("Connecting social account for platform: {} by user: {}", command.platform(), command.userId());

        try {
            // Exchange authorization code for access token
            OAuth2Token token = oauth2Port.exchangeCodeForToken(command.platform(), command.code(), command.redirectUri());

            if (token == null || token.getAccessToken() == null) {
                throw new OAuth2AuthenticationException("Failed to obtain access token from " + command.platform());
            }

            // Fetch account information from platform
            Map<String, Object> accountInfo = oauth2Port.fetchAccountInfo(command.platform(), token.getAccessToken());

            String platformAccountId = extractPlatformAccountId(accountInfo, command.platform());
            String accountName = extractAccountName(accountInfo);
            String accountHandle = extractAccountHandle(accountInfo);
            String accountAvatarUrl = extractAccountAvatarUrl(accountInfo);

            // Check if account is already connected
            boolean alreadyConnected = command.clientId() != null
                ? socialAccountPort.existsByOrganizationAndPlatformAndPlatformAccountId(
                    command.organizationId(), command.platform(), platformAccountId)
                : socialAccountPort.existsByOrganizationAndPlatformAndPlatformAccountId(
                    command.organizationId(), command.platform(), platformAccountId);

            if (alreadyConnected) {
                throw new SocialAccountAlreadyConnectedException(
                    String.format("Account %s on %s is already connected", platformAccountId, command.platform())
                );
            }

            // Create and save social account
            SocialAccount account = SocialAccount.builder()
                .userId(command.userId())
                .organizationId(command.organizationId())
                .clientId(command.clientId())
                .platform(command.platform())
                .platformAccountId(platformAccountId)
                .accountName(accountName)
                .accountHandle(accountHandle)
                .accountAvatarUrl(accountAvatarUrl)
                .token(token)
                .scopes(command.scopes())
                .platformMetadata(accountInfo)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            SocialAccount savedAccount = socialAccountPort.save(account);

            log.info("Successfully connected social account: {} on platform: {}", platformAccountId, command.platform());

            return savedAccount;

        } catch (OAuth2AuthenticationException | SocialAccountAlreadyConnectedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect social account for platform: {}", command.platform(), e);
            throw new OAuth2AuthenticationException("Failed to connect social account: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractPlatformAccountId(Map<String, Object> accountInfo, SocialPlatform platform) {
        return switch (platform) {
            case LINKEDIN -> (String) accountInfo.getOrDefault("id", accountInfo.get("sub"));
            case TWITTER -> {
                Map<String, Object> data = (Map<String, Object>) accountInfo.get("data");
                yield data != null ? (String) data.get("id") : null;
            }
            case INSTAGRAM -> (String) accountInfo.get("id");
            case YOUTUBE -> (String) accountInfo.get("id");
            case TIKTOK -> {
                Map<String, Object> data = (Map<String, Object>) accountInfo.get("data");
                if (data != null) {
                    Map<String, Object> user = (Map<String, Object>) data.get("user");
                    yield user != null ? (String) user.get("union_id") : null;
                }
                yield null;
            }
        };
    }

    private String extractAccountName(Map<String, Object> accountInfo) {
        if (accountInfo.containsKey("name")) {
            return (String) accountInfo.get("name");
        }
        if (accountInfo.containsKey("display_name")) {
            return (String) accountInfo.get("display_name");
        }
        if (accountInfo.containsKey("title")) {
            return (String) accountInfo.get("title");
        }
        return null;
    }

    private String extractAccountHandle(Map<String, Object> accountInfo) {
        if (accountInfo.containsKey("username")) {
            return (String) accountInfo.get("username");
        }
        if (accountInfo.containsKey("screen_name")) {
            return (String) accountInfo.get("screen_name");
        }
        if (accountInfo.containsKey("handle")) {
            return (String) accountInfo.get("handle");
        }
        return null;
    }

    private String extractAccountAvatarUrl(Map<String, Object> accountInfo) {
        if (accountInfo.containsKey("picture")) {
            return (String) accountInfo.get("picture");
        }
        if (accountInfo.containsKey("profile_image_url")) {
            return (String) accountInfo.get("profile_image_url");
        }
        if (accountInfo.containsKey("avatar_url")) {
            return (String) accountInfo.get("avatar_url");
        }
        return null;
    }
}
