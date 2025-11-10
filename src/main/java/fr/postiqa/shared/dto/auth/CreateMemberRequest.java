package fr.postiqa.shared.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a member directly (without invitation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private UUID clientId;  // Optional: for agency scope
    private UUID managerId;  // Optional: assign manager
    private String position;
    private String title;
    private Boolean sendCredentials;  // Send credentials via email
}
