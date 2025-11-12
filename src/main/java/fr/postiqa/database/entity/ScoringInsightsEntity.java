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
 * ScoringInsights entity representing scores and actionable insights.
 * Contains benchmarking, maturity assessment, and prioritized recommendations.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "scoring_insights", indexes = {
    @Index(name = "idx_scoring_insights_user_profile_analysis_id", columnList = "user_profile_analysis_id"),
    @Index(name = "idx_scoring_insights_overall_score", columnList = "overall_content_quality_score"),
    @Index(name = "idx_scoring_insights_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringInsightsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_analysis_id", nullable = false)
    private UserProfileAnalysisEntity userProfileAnalysis;

    // Individual scores (1-10)
    @Column(name = "overall_content_quality_score")
    private Integer overallContentQualityScore;

    @Column(name = "brand_consistency_score")
    private Integer brandConsistencyScore;

    @Column(name = "engagement_effectiveness_score")
    private Integer engagementEffectivenessScore;

    @Column(name = "thought_leadership_score")
    private Integer thoughtLeadershipScore;

    @Column(name = "authenticity_score")
    private Integer authenticityScore;

    @Column(name = "visual_branding_score")
    private Integer visualBrandingScore;

    @Column(name = "posting_consistency_score")
    private Integer postingConsistencyScore;

    @Column(name = "audience_building_score")
    private Integer audienceBuildingScore;

    @Column(name = "conversion_optimization_score")
    private Integer conversionOptimizationScore;

    @Column(name = "innovation_score")
    private Integer innovationScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "all_scores", columnDefinition = "jsonb")
    private Map<String, Object> allScores; // All scores in a map for easy access

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benchmarking", columnDefinition = "jsonb")
    private Map<String, Object> benchmarking; // content maturity level, percentile, assessment

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actionable_insights", columnDefinition = "jsonb")
    private Map<String, Object> actionableInsights; // List of prioritized insights with impact/effort

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_opportunities", columnDefinition = "jsonb")
    private Map<String, Object> contentOpportunities; // Specific opportunities per platform

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
