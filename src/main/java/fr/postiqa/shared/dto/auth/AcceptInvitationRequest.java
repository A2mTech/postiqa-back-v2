package fr.postiqa.shared.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for accepting an organization invitation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    private String password;  // Optional: if user doesn't exist yet
    private String firstName;
    private String lastName;
}
