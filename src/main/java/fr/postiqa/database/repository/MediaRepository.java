package fr.postiqa.database.repository;

import fr.postiqa.database.entity.MediaEntity;
import fr.postiqa.database.entity.PostEntity;
import fr.postiqa.shared.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MediaEntity.
 * Provides data access methods for media file management.
 */
@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {

    /**
     * Find all media for a specific post
     */
    List<MediaEntity> findByPost(PostEntity post);

    /**
     * Find all media for a specific post by ID
     */
    List<MediaEntity> findByPostId(UUID postId);

    /**
     * Find media by ID and post ID (for authorization)
     */
    Optional<MediaEntity> findByIdAndPostId(UUID id, UUID postId);

    /**
     * Find media by storage key
     */
    Optional<MediaEntity> findByStorageKey(String storageKey);

    /**
     * Find all media by type
     */
    List<MediaEntity> findByType(MediaType type);

    /**
     * Find all media by type for a specific post
     */
    List<MediaEntity> findByPostIdAndType(UUID postId, MediaType type);

    /**
     * Find all media created after a certain date
     */
    @Query("SELECT m FROM MediaEntity m WHERE m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<MediaEntity> findMediaCreatedSince(@Param("since") Instant since);

    /**
     * Find media by post and order by creation date
     */
    @Query("SELECT m FROM MediaEntity m WHERE m.post.id = :postId ORDER BY m.createdAt ASC")
    List<MediaEntity> findByPostIdOrderByCreatedAtAsc(@Param("postId") UUID postId);

    /**
     * Find all media for posts in an organization
     */
    @Query("""
        SELECT m FROM MediaEntity m
        JOIN m.post p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.organization.id = :organizationId
        ORDER BY m.createdAt DESC
        """)
    List<MediaEntity> findMediaByOrganization(@Param("organizationId") UUID organizationId);

    /**
     * Find all media for posts in a client
     */
    @Query("""
        SELECT m FROM MediaEntity m
        JOIN m.post p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.client.id = :clientId
        ORDER BY m.createdAt DESC
        """)
    List<MediaEntity> findMediaByClient(@Param("clientId") UUID clientId);

    /**
     * Count media by post
     */
    long countByPostId(UUID postId);

    /**
     * Count media by type
     */
    long countByType(MediaType type);

    /**
     * Calculate total file size for a post
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaEntity m WHERE m.post.id = :postId")
    Long getTotalFileSizeByPost(@Param("postId") UUID postId);

    /**
     * Calculate total file size for an organization
     */
    @Query("""
        SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaEntity m
        JOIN m.post p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.organization.id = :organizationId
        """)
    Long getTotalFileSizeByOrganization(@Param("organizationId") UUID organizationId);

    /**
     * Calculate total file size for a client
     */
    @Query("""
        SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaEntity m
        JOIN m.post p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.client.id = :clientId
        """)
    Long getTotalFileSizeByClient(@Param("clientId") UUID clientId);

    /**
     * Check if media exists by storage key
     */
    boolean existsByStorageKey(String storageKey);

    /**
     * Delete all media for a specific post
     */
    void deleteByPostId(UUID postId);

    /**
     * Find orphaned media (media without post) - for cleanup jobs
     */
    @Query("SELECT m FROM MediaEntity m WHERE m.post IS NULL")
    List<MediaEntity> findOrphanedMedia();

    /**
     * Find media older than a specific date for cleanup
     */
    @Query("""
        SELECT m FROM MediaEntity m
        WHERE m.createdAt < :threshold
        AND m.post.status IN ('CANCELLED', 'FAILED')
        """)
    List<MediaEntity> findMediaForCleanup(@Param("threshold") Instant threshold);
}
