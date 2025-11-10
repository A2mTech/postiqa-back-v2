package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.shared.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Use case for generating OAuth2 authorization URL.
 * Generates the URL that users will use to authorize the application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateAuthorizationUrlUseCase {

    private final OAuth2Port oauth2Port;

    /**
     * Execute the use case.
     *
     * @param platform Social media platform
     * @param redirectUri Redirect URI after authorization
     * @param scopes Requested OAuth2 scopes
     * @return OAuth2 authorization URL with state parameter
     */
    public OAuth2AuthorizationUrl execute(
        SocialPlatform platform,
        String redirectUri,
        String[] scopes
    ) {
        log.info("Generating authorization URL for platform: {}", platform);

        // Generate unique state for CSRF protection
        String state = UUID.randomUUID().toString();

        // Generate authorization URL using OAuth2 port
        OAuth2AuthorizationUrl authUrl = oauth2Port.generateAuthorizationUrl(
            platform,
            redirectUri,
            state,
            scopes
        );

        log.info("Generated authorization URL for platform: {} with state: {}", platform, state);

        return authUrl;
    }
}
