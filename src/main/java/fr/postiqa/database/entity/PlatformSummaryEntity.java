package fr.postiqa.database.entity;

import fr.postiqa.shared.enums.SocialPlatform;
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
 * PlatformSummary entity representing the aggregated analysis for a specific platform.
 * Synthesizes profile and posts analysis into overall patterns and insights.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "platform_summaries", indexes = {
    @Index(name = "idx_platform_summaries_user_profile_analysis_id", columnList = "user_profile_analysis_id"),
    @Index(name = "idx_platform_summaries_platform", columnList = "platform"),
    @Index(name = "idx_platform_summaries_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_analysis_id", nullable = false)
    private UserProfileAnalysisEntity userProfileAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SocialPlatform platform;

    @Column(name = "total_posts_analyzed", nullable = false)
    @Builder.Default
    private Integer totalPostsAnalyzed = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_quality", columnDefinition = "jsonb")
    private Map<String, Object> profileQuality;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_patterns", columnDefinition = "jsonb")
    private Map<String, Object> contentPatterns; // posting frequency, content mix, performance

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "writing_style_profile", columnDefinition = "jsonb")
    private Map<String, Object> writingStyleProfile; // dominant tone, voice, signature elements

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_alignment", columnDefinition = "jsonb")
    private Map<String, Object> brandAlignment; // consistency with business, thought leadership

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_engagement", columnDefinition = "jsonb")
    private Map<String, Object> audienceEngagement; // engagement quality, community building

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "competitive_positioning", columnDefinition = "jsonb")
    private Map<String, Object> competitivePositioning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    private Map<String, Object> recommendations; // content strategy, posting optimization

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
