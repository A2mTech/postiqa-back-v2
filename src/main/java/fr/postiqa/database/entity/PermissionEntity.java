package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Permission entity representing granular permissions in the system.
 * Format: RESOURCE:ACTION (e.g., POST:CREATE, CLIENT:MANAGE, ANALYTICS:VIEW)
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_resource_action", columnList = "resource, action", unique = true),
    @Index(name = "idx_permissions_resource", columnList = "resource")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermissionEntity> rolePermissions = new HashSet<>();

    /**
     * Returns the permission name in format RESOURCE:ACTION
     */
    public String getPermissionName() {
        return resource + ":" + action;
    }
}
