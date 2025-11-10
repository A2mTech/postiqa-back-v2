package fr.postiqa.gateway.auth;

import fr.postiqa.database.entity.PermissionEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.entity.UserRoleEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom UserDetails implementation.
 * Wraps UserEntity and provides authorities (roles + permissions).
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final boolean emailVerified;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;
    private final List<ScopeInfo> scopes;

    public CustomUserDetails(UserEntity user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.emailVerified = user.getEmailVerified();
        this.enabled = user.getEnabled();
        this.accountNonLocked = !user.getAccountLocked();
        this.authorities = extractAuthorities(user);
        this.scopes = extractScopes(user);
    }

    /**
     * Extract authorities (ROLE_ prefixed roles + permissions)
     */
    private Collection<? extends GrantedAuthority> extractAuthorities(UserEntity user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add roles with ROLE_ prefix (Spring Security convention)
        user.getUserRoles().forEach(userRole -> {
            String roleName = "ROLE_" + userRole.getRole().getName();
            authorities.add(new SimpleGrantedAuthority(roleName));

            // Add permissions from role
            userRole.getRole().getRolePermissions().forEach(rolePermission -> {
                PermissionEntity permission = rolePermission.getPermission();
                String permissionName = permission.getPermissionName();
                authorities.add(new SimpleGrantedAuthority(permissionName));
            });
        });

        return authorities;
    }

    /**
     * Extract scopes (organization/client contexts)
     */
    private List<ScopeInfo> extractScopes(UserEntity user) {
        return user.getUserRoles().stream()
            .map(userRole -> new ScopeInfo(
                userRole.getRole().getName(),
                userRole.getOrganization() != null ? userRole.getOrganization().getId() : null,
                userRole.getClient() != null ? userRole.getClient().getId() : null
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Scope information for multi-tenant support
     */
    @Getter
    public static class ScopeInfo {
        private final String roleName;
        private final UUID organizationId;
        private final UUID clientId;

        public ScopeInfo(String roleName, UUID organizationId, UUID clientId) {
            this.roleName = roleName;
            this.organizationId = organizationId;
            this.clientId = clientId;
        }

        public String getRoleName() {
            return roleName;
        }

        public UUID getOrganizationId() {
            return organizationId;
        }

        public UUID getClientId() {
            return clientId;
        }
    }
}
