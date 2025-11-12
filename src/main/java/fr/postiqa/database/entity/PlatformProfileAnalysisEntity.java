package fr.postiqa.database.entity;

import fr.postiqa.shared.enums.SocialPlatform;
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
 * PlatformProfileAnalysis entity representing the analysis of a social media profile.
 * Contains profile picture, banner, and bio analysis for a specific platform.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "platform_profile_analyses", indexes = {
    @Index(name = "idx_platform_profile_analyses_user_profile_analysis_id", columnList = "user_profile_analysis_id"),
    @Index(name = "idx_platform_profile_analyses_platform", columnList = "platform"),
    @Index(name = "idx_platform_profile_analyses_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformProfileAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_analysis_id", nullable = false)
    private UserProfileAnalysisEntity userProfileAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SocialPlatform platform;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "follower_count")
    private Integer followerCount;

    @Column(name = "following_count")
    private Integer followingCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_picture_analysis", columnDefinition = "jsonb")
    private Map<String, Object> profilePictureAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "banner_analysis", columnDefinition = "jsonb")
    private Map<String, Object> bannerAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bio_analysis", columnDefinition = "jsonb")
    private Map<String, Object> bioAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_profile_data", columnDefinition = "jsonb")
    private Map<String, Object> rawProfileData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relationship to posts
    @OneToMany(mappedBy = "platformProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostAnalysisEntity> postAnalyses = new ArrayList<>();
}
