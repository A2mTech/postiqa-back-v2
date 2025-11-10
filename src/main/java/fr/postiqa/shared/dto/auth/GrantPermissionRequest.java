package fr.postiqa.shared.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for granting or revoking a custom permission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Permission ID is required")
    private UUID permissionId;

    @NotNull(message = "Granted flag is required")
    private Boolean granted;

    private String reason;
}
