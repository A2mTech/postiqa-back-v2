package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for User entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean emailVerified;
    private Boolean enabled;
    private Boolean accountLocked;
    private Instant lastLoginAt;
    private Instant createdAt;
    private List<UserRoleDto> roles;
}
