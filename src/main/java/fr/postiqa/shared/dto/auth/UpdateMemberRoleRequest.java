package fr.postiqa.shared.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating a member's role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleRequest {

    private UUID memberId;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private UUID clientId;  // Optional: update client scope for agencies
}
