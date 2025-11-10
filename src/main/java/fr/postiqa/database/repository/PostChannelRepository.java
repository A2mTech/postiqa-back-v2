package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PostChannelEntity;
import fr.postiqa.database.entity.PostEntity;
import fr.postiqa.database.entity.SocialAccountEntity;
import fr.postiqa.shared.enums.PostChannelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PostChannelEntity.
 * Provides data access methods for post-channel relationship management.
 */
@Repository
public interface PostChannelRepository extends JpaRepository<PostChannelEntity, UUID> {

    /**
     * Find all post channels for a specific post
     */
    List<PostChannelEntity> findByPost(PostEntity post);

    /**
     * Find all post channels for a specific post by ID
     */
    List<PostChannelEntity> findByPostId(UUID postId);

    /**
     * Find all post channels for a specific channel
     */
    List<PostChannelEntity> findByChannel(SocialAccountEntity channel);

    /**
     * Find all post channels for a specific channel by ID
     */
    List<PostChannelEntity> findByChannelId(UUID channelId);

    /**
     * Find post channel by post and channel
     */
    Optional<PostChannelEntity> findByPostAndChannel(PostEntity post, SocialAccountEntity channel);

    /**
     * Find post channel by post ID and channel ID
     */
    Optional<PostChannelEntity> findByPostIdAndChannelId(UUID postId, UUID channelId);

    /**
     * Find post channels by status
     */
    List<PostChannelEntity> findByStatus(PostChannelStatus status);

    /**
     * Find post channels by post and status
     */
    List<PostChannelEntity> findByPostIdAndStatus(UUID postId, PostChannelStatus status);

    /**
     * Find post channels by channel and status
     */
    List<PostChannelEntity> findByChannelIdAndStatus(UUID channelId, PostChannelStatus status);

    /**
     * Find pending post channels for scheduled posts ready to publish
     */
    @Query("""
        SELECT pc FROM PostChannelEntity pc
        WHERE pc.status = 'PENDING'
        AND pc.post.status = 'SCHEDULED'
        AND pc.post.scheduledFor <= :now
        """)
    List<PostChannelEntity> findPendingChannelsReadyForPublishing(@Param("now") Instant now);

    /**
     * Find failed post channels that can be retried
     */
    @Query("""
        SELECT pc FROM PostChannelEntity pc
        WHERE pc.status = 'FAILED'
        AND pc.channel.active = true
        ORDER BY pc.updatedAt DESC
        """)
    List<PostChannelEntity> findFailedChannelsForRetry();

    /**
     * Find post channels by external post ID
     */
    Optional<PostChannelEntity> findByExternalPostId(String externalPostId);

    /**
     * Find post channels by channel and published date range
     */
    @Query("""
        SELECT pc FROM PostChannelEntity pc
        WHERE pc.channel.id = :channelId
        AND pc.status = 'PUBLISHED'
        AND pc.publishedAt BETWEEN :startDate AND :endDate
        ORDER BY pc.publishedAt DESC
        """)
    List<PostChannelEntity> findPublishedByChannelAndDateRange(
        @Param("channelId") UUID channelId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Count post channels by status for a specific channel
     */
    long countByChannelIdAndStatus(UUID channelId, PostChannelStatus status);

    /**
     * Count post channels by status for a specific post
     */
    long countByPostIdAndStatus(UUID postId, PostChannelStatus status);

    /**
     * Check if a post has been published on any channel
     */
    @Query("""
        SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END
        FROM PostChannelEntity pc
        WHERE pc.post.id = :postId
        AND pc.status = 'PUBLISHED'
        """)
    boolean hasPostBeenPublished(@Param("postId") UUID postId);

    /**
     * Check if a post channel exists for post and channel
     */
    boolean existsByPostIdAndChannelId(UUID postId, UUID channelId);

    /**
     * Delete post channels by post ID
     */
    void deleteByPostId(UUID postId);

    /**
     * Delete post channel by post ID and channel ID
     */
    void deleteByPostIdAndChannelId(UUID postId, UUID channelId);

    /**
     * Find all post channels for posts in an organization
     */
    @Query("""
        SELECT pc FROM PostChannelEntity pc
        WHERE pc.channel.organization.id = :organizationId
        ORDER BY pc.createdAt DESC
        """)
    List<PostChannelEntity> findByOrganization(@Param("organizationId") UUID organizationId);

    /**
     * Find all post channels for posts in a client
     */
    @Query("""
        SELECT pc FROM PostChannelEntity pc
        WHERE pc.channel.client.id = :clientId
        ORDER BY pc.createdAt DESC
        """)
    List<PostChannelEntity> findByClient(@Param("clientId") UUID clientId);
}
