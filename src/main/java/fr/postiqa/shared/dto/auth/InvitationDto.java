package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for OrganizationInvitation entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDto {

    private UUID id;
    private String email;
    private OrganizationDto organization;
    private RoleDto role;
    private ClientDto client;
    private UserDto invitedBy;
    private String status;
    private Instant expiresAt;
    private Instant acceptedAt;
    private Instant createdAt;
}
