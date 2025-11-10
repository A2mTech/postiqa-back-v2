package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * RolePermission join table linking roles to permissions.
 * Defines what permissions each role has (RBAC).
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
    @Index(name = "idx_role_permissions_permission_id", columnList = "permission_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
