package fr.postiqa.features.socialaccounts.infrastructure.strategy;

import fr.postiqa.shared.enums.SocialPlatform;
import fr.postiqa.shared.exception.socialaccount.InvalidSocialPlatformException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for OAuth2 strategies.
 * Provides the appropriate strategy based on social platform.
 */
@Component
@Slf4j
public class OAuth2StrategyFactory {

    private final Map<SocialPlatform, OAuth2Strategy> strategies;

    public OAuth2StrategyFactory(List<OAuth2Strategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                OAuth2Strategy::getSupportedPlatform,
                Function.identity()
            ));

        log.info("Initialized OAuth2StrategyFactory with {} strategies", strategies.size());
    }

    /**
     * Get the OAuth2 strategy for a specific platform.
     */
    public OAuth2Strategy getStrategy(SocialPlatform platform) {
        OAuth2Strategy strategy = strategies.get(platform);

        if (strategy == null) {
            throw new InvalidSocialPlatformException("No OAuth2 strategy found for platform: " + platform);
        }

        return strategy;
    }

    /**
     * Check if a platform is supported.
     */
    public boolean isSupported(SocialPlatform platform) {
        return strategies.containsKey(platform);
    }
}
