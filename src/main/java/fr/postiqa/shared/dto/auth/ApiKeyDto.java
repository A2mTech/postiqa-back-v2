package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for API key (without plain-text key).
 * Used for listing API keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDto {

    private UUID id;
    private String name;
    private String description;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private Boolean active;
    private Instant createdAt;
}
