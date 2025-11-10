package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization invitation entity for inviting users to join an organization.
 * Contains unique token for invitation acceptance and expiration.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "organization_invitations", indexes = {
    @Index(name = "idx_org_inv_email", columnList = "email"),
    @Index(name = "idx_org_inv_org_id", columnList = "organization_id"),
    @Index(name = "idx_org_inv_token", columnList = "token", unique = true),
    @Index(name = "idx_org_inv_status", columnList = "status"),
    @Index(name = "idx_org_inv_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationInvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private UserEntity invitedBy;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private UserEntity acceptedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        EXPIRED,
        REVOKED
    }
}
