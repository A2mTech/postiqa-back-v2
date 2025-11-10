package fr.postiqa.features.socialaccounts.domain.model;

import fr.postiqa.shared.enums.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object representing an OAuth2 authorization URL.
 * Contains the URL and state parameter for OAuth2 flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthorizationUrl {

    private SocialPlatform platform;
    private String url;
    private String state;
    private String redirectUri;
}
