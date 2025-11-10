package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * UserRole join table with scopes for horizontal permissions.
 * Scopes define the organizational/client context for a role.
 * Example: User has AGENCY_ADMIN role scoped to organization X
 *          User has AGENCY_USER role scoped to client Y
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
    @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
    @Index(name = "idx_user_roles_organization_id", columnList = "organization_id"),
    @Index(name = "idx_user_roles_client_id", columnList = "client_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_role_org_client", columnNames = {"user_id", "role_id", "organization_id", "client_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
