package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Activity log entity for tracking user actions in the system.
 * Used for audit trail and supervision of member activities.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_org_id", columnList = "organization_id"),
    @Index(name = "idx_activity_client_id", columnList = "client_id"),
    @Index(name = "idx_activity_action", columnList = "action"),
    @Index(name = "idx_activity_resource_type", columnList = "resource_type"),
    @Index(name = "idx_activity_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogEntity {

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
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant timestamp;
}
