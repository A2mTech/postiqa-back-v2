package fr.postiqa.features.postmanagement.domain.port;

import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.vo.*;
import fr.postiqa.shared.enums.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for Post persistence operations.
 * Implemented by persistence adapters.
 */
public interface PostRepositoryPort {

    /**
     * Save a post (create or update)
     */
    Post save(Post post);

    /**
     * Find post by ID
     */
    Optional<Post> findById(PostId postId);

    /**
     * Find post by ID with authorization check
     */
    Optional<Post> findByIdAndOrganization(PostId postId, OrganizationId organizationId);

    /**
     * Find post by ID for a specific client (agency)
     */
    Optional<Post> findByIdAndClient(PostId postId, ClientId clientId);

    /**
     * Find all posts for an organization
     */
    List<Post> findByOrganization(OrganizationId organizationId, int page, int size);

    /**
     * Find all posts for a client
     */
    List<Post> findByClient(ClientId clientId, int page, int size);

    /**
     * Find posts by status for an organization
     */
    List<Post> findByOrganizationAndStatus(OrganizationId organizationId, PostStatus status, int page, int size);

    /**
     * Find posts by status for a client
     */
    List<Post> findByClientAndStatus(ClientId clientId, PostStatus status, int page, int size);

    /**
     * Find scheduled posts ready to publish
     */
    List<Post> findScheduledPostsReadyForPublishing(Instant now);

    /**
     * Delete a post
     */
    void delete(PostId postId);

    /**
     * Check if post exists and belongs to organization
     */
    boolean existsByIdAndOrganization(PostId postId, OrganizationId organizationId);

    /**
     * Check if post exists and belongs to client
     */
    boolean existsByIdAndClient(PostId postId, ClientId clientId);
}
