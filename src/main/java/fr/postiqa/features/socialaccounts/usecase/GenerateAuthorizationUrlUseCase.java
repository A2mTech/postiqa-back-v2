package fr.postiqa.features.socialaccounts.usecase;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Use case for generating OAuth2 authorization URL.
 * Generates the URL that users will use to authorize the application.
 */
@UseCase(
    value = "GenerateAuthorizationUrl",
    resourceType = "SOCIAL_ACCOUNT",
    description = "Generates OAuth2 authorization URL",
    logActivity = false
)
@RequiredArgsConstructor
@Slf4j
public class GenerateAuthorizationUrlUseCase implements fr.postiqa.shared.usecase.UseCase<GenerateAuthorizationUrlUseCase.GenerateAuthUrlCommand, OAuth2AuthorizationUrl> {

    private final OAuth2Port oauth2Port;

    /**
     * Command for generating authorization URL
     */
    public record GenerateAuthUrlCommand(
        SocialPlatform platform,
        String redirectUri,
        String[] scopes
    ) {}

    /**
     * Execute the use case.
     *
     * @param command Command with platform, redirectUri, and scopes
     * @return OAuth2 authorization URL with state parameter
     */
    public OAuth2AuthorizationUrl execute(GenerateAuthUrlCommand command) {
        log.info("Generating authorization URL for platform: {}", command.platform());

        // Generate unique state for CSRF protection
        String state = UUID.randomUUID().toString();

        // Generate authorization URL using OAuth2 port
        OAuth2AuthorizationUrl authUrl = oauth2Port.generateAuthorizationUrl(
            command.platform(),
            command.redirectUri(),
            state,
            command.scopes()
        );

        log.info("Generated authorization URL for platform: {} with state: {}", command.platform(), state);

        return authUrl;
    }
}
