package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for API key creation.
 * IMPORTANT: The plain-text key is only returned once during creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private UUID id;
    private String name;
    private String description;
    private String key; // Plain-text key (only returned once)
    private Instant expiresAt;
    private Instant createdAt;
}
