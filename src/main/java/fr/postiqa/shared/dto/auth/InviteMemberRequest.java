package fr.postiqa.shared.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for inviting a member to an organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private UUID clientId;  // Optional: for agency scope
    private UUID managerId;  // Optional: assign manager
    private String position;
    private String title;
}
