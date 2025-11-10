package fr.postiqa.shared.dto.socialaccount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response after successfully connecting a social account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectSocialAccountResponse {

    private SocialAccountDto account;
    private String message;
    private Boolean success;
}
