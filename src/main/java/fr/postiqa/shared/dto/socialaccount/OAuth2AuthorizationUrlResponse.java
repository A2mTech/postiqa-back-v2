package fr.postiqa.shared.dto.socialaccount;

import fr.postiqa.shared.enums.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing OAuth2 authorization URL for user to connect social account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthorizationUrlResponse {

    private SocialPlatform platform;
    private String authorizationUrl;
    private String state;
    private String message;
}
