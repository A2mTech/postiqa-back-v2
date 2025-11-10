package fr.postiqa.shared.dto.socialaccount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response after refreshing a social account token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    private UUID accountId;
    private Boolean success;
    private Instant newTokenExpiresAt;
    private String message;
}
