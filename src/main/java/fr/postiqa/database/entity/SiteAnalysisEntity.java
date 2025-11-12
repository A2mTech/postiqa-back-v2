package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SiteAnalysis entity representing the analysis of a website.
 * Contains business identity, value proposition, target audience, brand identity, etc.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "site_analyses", indexes = {
    @Index(name = "idx_site_analyses_user_profile_analysis_id", columnList = "user_profile_analysis_id"),
    @Index(name = "idx_site_analyses_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_analysis_id", nullable = false)
    private UserProfileAnalysisEntity userProfileAnalysis;

    @Column(name = "site_url", nullable = false, length = 500)
    private String siteUrl;

    @Column(name = "total_pages", nullable = false)
    private Integer totalPages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_identity", columnDefinition = "jsonb")
    private Map<String, Object> businessIdentity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_service", columnDefinition = "jsonb")
    private Map<String, Object> productService;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_audience", columnDefinition = "jsonb")
    private Map<String, Object> targetAudience;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_model", columnDefinition = "jsonb")
    private Map<String, Object> businessModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stage", columnDefinition = "jsonb")
    private Map<String, Object> stage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_identity", columnDefinition = "jsonb")
    private Map<String, Object> brandIdentity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_proof", columnDefinition = "jsonb")
    private Map<String, Object> socialProof;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_strategy", columnDefinition = "jsonb")
    private Map<String, Object> contentStrategy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ctas_analysis", columnDefinition = "jsonb")
    private Map<String, Object> ctasAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "technical_stack", columnDefinition = "jsonb")
    private Map<String, Object> technicalStack;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
