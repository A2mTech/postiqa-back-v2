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
 * PostAnalysis entity representing the granular analysis of a single social media post.
 * Supports all post types: text, image, carousel, video, thread.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "post_analyses", indexes = {
    @Index(name = "idx_post_analyses_platform_profile_id", columnList = "platform_profile_id"),
    @Index(name = "idx_post_analyses_post_type", columnList = "post_type"),
    @Index(name = "idx_post_analyses_published_at", columnList = "published_at"),
    @Index(name = "idx_post_analyses_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_profile_id", nullable = false)
    private PlatformProfileAnalysisEntity platformProfile;

    @Column(name = "post_id", length = 255)
    private String postId;

    @Column(name = "post_url", length = 500)
    private String postUrl;

    @Column(name = "post_type", nullable = false, length = 50)
    private String postType; // text, image, carousel, video, thread

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "published_at")
    private Instant publishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "engagement", columnDefinition = "jsonb")
    private Map<String, Object> engagement; // likes, comments, shares, views

    // Analysis results
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_analysis", columnDefinition = "jsonb")
    private Map<String, Object> contentAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure_analysis", columnDefinition = "jsonb")
    private Map<String, Object> structureAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "writing_style", columnDefinition = "jsonb")
    private Map<String, Object> writingStyle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formatting", columnDefinition = "jsonb")
    private Map<String, Object> formatting;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_elements", columnDefinition = "jsonb")
    private Map<String, Object> contentElements; // hashtags, mentions, links

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "engagement_analysis", columnDefinition = "jsonb")
    private Map<String, Object> engagementAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_alignment", columnDefinition = "jsonb")
    private Map<String, Object> brandAlignment;

    // Type-specific analysis
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visual_analysis", columnDefinition = "jsonb")
    private Map<String, Object> visualAnalysis; // For image/carousel/video

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "carousel_analysis", columnDefinition = "jsonb")
    private Map<String, Object> carouselAnalysis; // For carousels (multi-slide)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "video_analysis", columnDefinition = "jsonb")
    private Map<String, Object> videoAnalysis; // For videos (transcription, frames, narrative)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "thread_analysis", columnDefinition = "jsonb")
    private Map<String, Object> threadAnalysis; // For threads (multi-tweet structure)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "replicability_insights", columnDefinition = "jsonb")
    private Map<String, Object> replicabilityInsights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_post_data", columnDefinition = "jsonb")
    private Map<String, Object> rawPostData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
