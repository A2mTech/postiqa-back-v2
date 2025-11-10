package fr.postiqa.features.socialaccounts.adapter.out;

import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.features.socialaccounts.domain.port.OAuth2Port;
import fr.postiqa.features.socialaccounts.infrastructure.strategy.OAuth2Strategy;
import fr.postiqa.features.socialaccounts.infrastructure.strategy.OAuth2StrategyFactory;
import fr.postiqa.shared.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter implementing OAuth2Port using OAuth2 strategies.
 * Delegates OAuth2 operations to platform-specific strategies.
 */
@Component
@RequiredArgsConstructor
public class OAuth2Adapter implements OAuth2Port {

    private final OAuth2StrategyFactory strategyFactory;

    @Override
    public OAuth2AuthorizationUrl generateAuthorizationUrl(
        SocialPlatform platform,
        String redirectUri,
        String state,
        String[] scopes
    ) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
        return strategy.generateAuthorizationUrl(redirectUri, state, scopes);
    }

    @Override
    public OAuth2Token exchangeCodeForToken(
        SocialPlatform platform,
        String code,
        String redirectUri
    ) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
        return strategy.exchangeCodeForToken(code, redirectUri);
    }

    @Override
    public OAuth2Token refreshToken(
        SocialPlatform platform,
        String refreshToken
    ) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
        return strategy.refreshToken(refreshToken);
    }

    @Override
    public void revokeToken(
        SocialPlatform platform,
        String accessToken
    ) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
        strategy.revokeToken(accessToken);
    }

    @Override
    public Map<String, Object> fetchAccountInfo(
        SocialPlatform platform,
        String accessToken
    ) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(platform);
        return strategy.fetchAccountInfo(accessToken);
    }
}
