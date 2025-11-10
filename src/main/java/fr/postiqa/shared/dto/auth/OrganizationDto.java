package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Organization entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

    private UUID id;
    private String name;
    private String type; // BUSINESS or AGENCY
    private String subscriptionTier;
    private Map<String, Object> metadata;
    private Boolean active;
    private Instant createdAt;
}
