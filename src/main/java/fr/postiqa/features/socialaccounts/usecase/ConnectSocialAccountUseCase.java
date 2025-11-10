package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.enums.SocialPlatform;
import fr.postiqa.shared.exception.socialaccount.OAuth2AuthenticationException;
import fr.postiqa.shared.exception.socialaccount.SocialAccountAlreadyConnectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for connecting a social media account.
 * Handles OAuth2 callback and saves the connected account.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectSocialAccountUseCase {

    private final OAuth2Port oauth2Port;
    private final SocialAccountPort socialAccountPort;

    /**
     * Execute the use case.
     *
     * @param platform Social media platform
     * @param code Authorization code from OAuth2 callback
     * @param redirectUri Redirect URI used during authorization
     * @param userId User ID connecting the account
     * @param organizationId Organization ID
     * @param clientId Client ID (optional, for agencies)
     * @param scopes Granted OAuth2 scopes
     * @return Connected social account
     */
    @Transactional
    public SocialAccount execute(
        SocialPlatform platform,
        String code,
        String redirectUri,
        UUID userId,
        UUID organizationId,
        UUID clientId,
        String scopes
    ) {
        log.info("Connecting social account for platform: {} by user: {}", platform, userId);

        try {
            // Exchange authorization code for access token
            OAuth2Token token = oauth2Port.exchangeCodeForToken(platform, code, redirectUri);

            if (token == null || token.getAccessToken() == null) {
                throw new OAuth2AuthenticationException("Failed to obtain access token from " + platform);
            }

            // Fetch account information from platform
            Map<String, Object> accountInfo = oauth2Port.fetchAccountInfo(platform, token.getAccessToken());

            String platformAccountId = extractPlatformAccountId(accountInfo, platform);
            String accountName = extractAccountName(accountInfo);
            String accountHandle = extractAccountHandle(accountInfo);
            String accountAvatarUrl = extractAccountAvatarUrl(accountInfo);

            // Check if account is already connected
            boolean alreadyConnected = clientId != null
                ? socialAccountPort.existsByOrganizationAndPlatformAndPlatformAccountId(
                    organizationId, platform, platformAccountId)
                : socialAccountPort.existsByOrganizationAndPlatformAndPlatformAccountId(
                    organizationId, platform, platformAccountId);

            if (alreadyConnected) {
                throw new SocialAccountAlreadyConnectedException(
                    String.format("Account %s on %s is already connected", platformAccountId, platform)
                );
            }

            // Create and save social account
            SocialAccount account = SocialAccount.builder()
                .userId(userId)
                .organizationId(organizationId)
                .clientId(clientId)
                .platform(platform)
                .platformAccountId(platformAccountId)
                .accountName(accountName)
                .accountHandle(accountHandle)
                .accountAvatarUrl(accountAvatarUrl)
                .token(token)
                .scopes(scopes)
                .platformMetadata(accountInfo)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            SocialAccount savedAccount = socialAccountPort.save(account);

            log.info("Successfully connected social account: {} on platform: {}", platformAccountId, platform);

            return savedAccount;

        } catch (OAuth2AuthenticationException | SocialAccountAlreadyConnectedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect social account for platform: {}", platform, e);
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
