package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Organization entity representing a business or agency in the system.
 * Type: BUSINESS (manages own account) or AGENCY (manages multiple clients)
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_type", columnList = "type"),
    @Index(name = "idx_organizations_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationType type;

    @Column(name = "subscription_tier", length = 50)
    private String subscriptionTier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRoleEntity> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ClientEntity> clients = new HashSet<>();

    public enum OrganizationType {
        BUSINESS,  // Manages own social media account
        AGENCY     // Manages multiple client accounts
    }
}
