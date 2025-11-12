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
 * CrossReferenceAnalysis entity representing the cross-platform aggregation and global profile.
 * Synthesizes all platform summaries and site analysis into unified insights.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "cross_reference_analyses", indexes = {
    @Index(name = "idx_cross_reference_analyses_user_profile_analysis_id", columnList = "user_profile_analysis_id"),
    @Index(name = "idx_cross_reference_analyses_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrossReferenceAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_analysis_id", nullable = false)
    private UserProfileAnalysisEntity userProfileAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "identity", columnDefinition = "jsonb")
    private Map<String, Object> identity; // name, professional title, expertise

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_presence", columnDefinition = "jsonb")
    private Map<String, Object> businessPresence; // business type, value prop, target audience

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personal_brand", columnDefinition = "jsonb")
    private Map<String, Object> personalBrand; // positioning, consistency, maturity

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_dna", columnDefinition = "jsonb")
    private Map<String, Object> contentDNA; // unified voice, themes, formats, authenticity

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cross_platform_insights", columnDefinition = "jsonb")
    private Map<String, Object> crossPlatformInsights; // platform strategies, repurposing patterns

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_relationship", columnDefinition = "jsonb")
    private Map<String, Object> audienceRelationship; // engagement style, community building

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "growth_trajectory", columnDefinition = "jsonb")
    private Map<String, Object> growthTrajectory; // content evolution, consistency, quality progression

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private Map<String, Object> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "jsonb")
    private Map<String, Object> weaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opportunities", columnDefinition = "jsonb")
    private Map<String, Object> opportunities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strategic_recommendations", columnDefinition = "jsonb")
    private Map<String, Object> strategicRecommendations;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
