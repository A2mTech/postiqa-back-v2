package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UserProfileAnalysis entity representing a complete ultra-deep analysis of a user's profile.
 * Links to workflow execution and contains all analysis results.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "user_profile_analyses", indexes = {
    @Index(name = "idx_user_profile_analyses_user_id", columnList = "user_id"),
    @Index(name = "idx_user_profile_analyses_organization_id", columnList = "organization_id"),
    @Index(name = "idx_user_profile_analyses_client_id", columnList = "client_id"),
    @Index(name = "idx_user_profile_analyses_workflow_instance_id", columnList = "workflow_instance_id"),
    @Index(name = "idx_user_profile_analyses_status", columnList = "status"),
    @Index(name = "idx_user_profile_analyses_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Column(name = "workflow_instance_id", nullable = false, length = 255)
    private String workflowInstanceId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "site_url", length = 500)
    private String siteUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "platforms", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> platforms = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_profile", columnDefinition = "jsonb")
    private Map<String, Object> finalProfile;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Relationships
    @OneToOne(mappedBy = "userProfileAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private SiteAnalysisEntity siteAnalysis;

    @OneToMany(mappedBy = "userProfileAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlatformProfileAnalysisEntity> platformProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "userProfileAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlatformSummaryEntity> platformSummaries = new ArrayList<>();

    @OneToOne(mappedBy = "userProfileAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private CrossReferenceAnalysisEntity crossReferenceAnalysis;

    @OneToOne(mappedBy = "userProfileAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private ScoringInsightsEntity scoringInsights;
}
