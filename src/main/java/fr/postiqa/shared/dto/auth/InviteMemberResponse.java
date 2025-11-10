package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO after inviting a member.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberResponse {

    private UUID invitationId;
    private String email;
    private String organizationName;
    private String roleName;
    private Instant expiresAt;
    private String message;
}
