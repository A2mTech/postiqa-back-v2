package fr.postiqa.gateway.auth.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Custom OAuth2 User Service.
 * Loads OAuth2 user information from the provider.
 * Note: User creation/linking is handled in OAuth2AuthenticationSuccessHandler via HandleOAuth2LoginUseCase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user from OAuth2 provider using default implementation
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.debug("Loaded OAuth2 user from provider: {}", provider);

        // Return OAuth2User (further processing happens in success handler)
        return oauth2User;
    }
}
