package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * User permission override entity for granular custom permissions.
 * Allows granting or revoking specific permissions for a user in an organization
 * beyond their role-based permissions.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "user_permission_overrides", indexes = {
    @Index(name = "idx_perm_override_user_id", columnList = "user_id"),
    @Index(name = "idx_perm_override_org_id", columnList = "organization_id"),
    @Index(name = "idx_perm_override_perm_id", columnList = "permission_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_perm_org", columnNames = {"user_id", "organization_id", "permission_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    @Column(nullable = false)
    private Boolean granted;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private UserEntity grantedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
