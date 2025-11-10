package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization member entity representing a user's membership in an organization.
 * Includes hierarchical relationship (manager) and position information.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "organization_members", indexes = {
    @Index(name = "idx_org_members_user_id", columnList = "user_id"),
    @Index(name = "idx_org_members_org_id", columnList = "organization_id"),
    @Index(name = "idx_org_members_manager_id", columnList = "manager_id"),
    @Index(name = "idx_org_members_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_member_user_org", columnNames = {"user_id", "organization_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberEntity {

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
    @JoinColumn(name = "manager_id")
    private UserEntity manager;

    @Column(length = 100)
    private String position;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private UserEntity invitedBy;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum MemberStatus {
        ACTIVE,
        INVITED,
        SUSPENDED,
        REMOVED
    }
}
