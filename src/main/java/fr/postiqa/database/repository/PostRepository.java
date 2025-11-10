package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PostEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.shared.enums.PostStatus;
import fr.postiqa.shared.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PostEntity.
 * Provides data access methods for post management.
 */
@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    /**
     * Find post by ID and created by user ID (for authorization)
     */
    Optional<PostEntity> findByIdAndCreatedById(UUID id, UUID userId);

    /**
     * Find all posts created by a specific user
     */
    List<PostEntity> findByCreatedBy(UserEntity createdBy);

    /**
     * Find all posts created by a specific user with pagination
     */
    Page<PostEntity> findByCreatedById(UUID userId, Pageable pageable);

    /**
     * Find posts by status
     */
    List<PostEntity> findByStatus(PostStatus status);

    /**
     * Find posts by status with pagination
     */
    Page<PostEntity> findByStatus(PostStatus status, Pageable pageable);

    /**
     * Find posts by type
     */
    List<PostEntity> findByType(PostType type);

    /**
     * Find posts by created by user and status
     */
    Page<PostEntity> findByCreatedByIdAndStatus(UUID userId, PostStatus status, Pageable pageable);

    /**
     * Find posts by created by user and type
     */
    Page<PostEntity> findByCreatedByIdAndType(UUID userId, PostType type, Pageable pageable);

    /**
     * Find scheduled posts that need to be published
     */
    @Query("SELECT p FROM PostEntity p WHERE p.status = 'SCHEDULED' AND p.scheduledFor <= :now")
    List<PostEntity> findScheduledPostsReadyForPublishing(@Param("now") Instant now);

    /**
     * Find posts scheduled between two dates
     */
    @Query("SELECT p FROM PostEntity p WHERE p.status = 'SCHEDULED' AND p.scheduledFor BETWEEN :startDate AND :endDate")
    List<PostEntity> findPostsScheduledBetween(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find posts created by user in a date range
     */
    @Query("SELECT p FROM PostEntity p WHERE p.createdBy.id = :userId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<PostEntity> findPostsByUserAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Count posts by user and status
     */
    long countByCreatedByIdAndStatus(UUID userId, PostStatus status);

    /**
     * Count posts by user
     */
    long countByCreatedById(UUID userId);

    /**
     * Find posts by organization (via channels)
     */
    @Query("""
        SELECT DISTINCT p FROM PostEntity p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.organization.id = :organizationId
        ORDER BY p.createdAt DESC
        """)
    Page<PostEntity> findPostsByOrganization(@Param("organizationId") UUID organizationId, Pageable pageable);

    /**
     * Find posts by client (via channels)
     */
    @Query("""
        SELECT DISTINCT p FROM PostEntity p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.client.id = :clientId
        ORDER BY p.createdAt DESC
        """)
    Page<PostEntity> findPostsByClient(@Param("clientId") UUID clientId, Pageable pageable);

    /**
     * Find posts by organization and status
     */
    @Query("""
        SELECT DISTINCT p FROM PostEntity p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.organization.id = :organizationId
        AND p.status = :status
        ORDER BY p.createdAt DESC
        """)
    Page<PostEntity> findPostsByOrganizationAndStatus(
        @Param("organizationId") UUID organizationId,
        @Param("status") PostStatus status,
        Pageable pageable
    );

    /**
     * Find posts by client and status
     */
    @Query("""
        SELECT DISTINCT p FROM PostEntity p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE c.client.id = :clientId
        AND p.status = :status
        ORDER BY p.createdAt DESC
        """)
    Page<PostEntity> findPostsByClientAndStatus(
        @Param("clientId") UUID clientId,
        @Param("status") PostStatus status,
        Pageable pageable
    );

    /**
     * Check if post exists by ID and organization ID (for authorization)
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM PostEntity p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE p.id = :postId
        AND c.organization.id = :organizationId
        """)
    boolean existsByIdAndOrganizationId(@Param("postId") UUID postId, @Param("organizationId") UUID organizationId);

    /**
     * Check if post exists by ID and client ID (for agency tenant isolation)
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM PostEntity p
        JOIN p.channels pc
        JOIN pc.channel c
        WHERE p.id = :postId
        AND c.client.id = :clientId
        """)
    boolean existsByIdAndClientId(@Param("postId") UUID postId, @Param("clientId") UUID clientId);
}
