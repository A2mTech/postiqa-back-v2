package fr.postiqa.shared.dto.socialaccount;

import fr.postiqa.shared.enums.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response after testing a social account connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestConnectionResponse {

    private UUID accountId;
    private SocialPlatform platform;
    private Boolean isValid;
    private String message;
    private String errorDetails;
}
