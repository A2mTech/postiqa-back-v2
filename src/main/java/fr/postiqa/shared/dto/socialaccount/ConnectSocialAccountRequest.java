package fr.postiqa.shared.dto.socialaccount;

import fr.postiqa.shared.enums.SocialPlatform;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to complete OAuth2 callback and connect social account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectSocialAccountRequest {

    @NotNull(message = "Platform is required")
    private SocialPlatform platform;

    @NotNull(message = "Authorization code is required")
    private String code;

    @NotNull(message = "State is required")
    private String state;

    private String error;
    private String errorDescription;
}
