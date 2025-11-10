package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for OrganizationMember entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberDto {

    private UUID id;
    private UserDto user;
    private OrganizationDto organization;
    private UserDto manager;
    private String position;
    private String title;
    private String status;
    private UserDto invitedBy;
    private Instant joinedAt;
    private Instant createdAt;
    private List<UserRoleDto> roles;
    private List<PermissionOverrideDto> permissionOverrides;
}
