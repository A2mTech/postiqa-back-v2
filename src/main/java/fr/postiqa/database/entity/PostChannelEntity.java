package fr.postiqa.database.entity;

import fr.postiqa.shared.enums.PostChannelStatus;
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
 * PostChannel entity representing the many-to-many relationship between posts and social accounts.
 * Tracks publication status per platform for cross-posting support.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "post_channels", indexes = {
    @Index(name = "idx_post_channels_post_id", columnList = "post_id"),
    @Index(name = "idx_post_channels_channel_id", columnList = "channel_id"),
    @Index(name = "idx_post_channels_status", columnList = "status"),
    @Index(name = "idx_post_channels_published_at", columnList = "published_at"),
    @Index(name = "idx_post_channels_external_post_id", columnList = "external_post_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private SocialAccountEntity channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostChannelStatus status = PostChannelStatus.PENDING;

    @Column(name = "external_post_id", length = 255)
    private String externalPostId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
