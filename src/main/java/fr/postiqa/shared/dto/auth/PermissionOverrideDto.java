package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for UserPermissionOverride entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionOverrideDto {

    private UUID id;
    private PermissionDto permission;
    private Boolean granted;
    private String reason;
    private UserDto grantedBy;
    private Instant createdAt;
}
