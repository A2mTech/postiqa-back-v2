package fr.postiqa.features.socialaccounts.adapter.out;

import fr.postiqa.features.socialaccounts.domain.model.ConnectionTestResult;
import fr.postiqa.features.socialaccounts.domain.port.SocialPlatformApiPort;
import fr.postiqa.features.socialaccounts.infrastructure.strategy.OAuth2Strategy;
import fr.postiqa.features.socialaccounts.infrastructure.strategy.OAuth2StrategyFactory;
import fr.postiqa.shared.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing SocialPlatformApiPort.
 * Tests connections to social media platforms.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialPlatformApiAdapter implements SocialPlatformApiPort {

    private final OAuth2StrategyFactory strategyFactory;

    @Override
    public ConnectionTestResult testConnection(
        SocialPlatform platform,
        String accessToken
    ) {
        try {
            OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
            boolean isValid = strategy.testConnection(accessToken);

            if (isValid) {
                return ConnectionTestResult.success("Connection to " + platform + " is valid");
            } else {
                return ConnectionTestResult.failure(
                    "Connection to " + platform + " failed",
                    "Token may be invalid or expired"
                );
            }
        } catch (Exception e) {
            log.error("Error testing connection to {}", platform, e);
            return ConnectionTestResult.failure(
                "Connection test failed",
                e.getMessage()
            );
        }
    }

    @Override
    public boolean isTokenValid(
        SocialPlatform platform,
        String accessToken
    ) {
        try {
            OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
            return strategy.testConnection(accessToken);
        } catch (Exception e) {
            log.error("Error validating token for {}", platform, e);
            return false;
        }
    }
}
