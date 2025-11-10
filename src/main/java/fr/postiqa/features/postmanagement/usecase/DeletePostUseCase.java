package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.*;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use case for deleting a post.
 * Deletes associated media from storage and publishes event.
 */
@Component
public class DeletePostUseCase {

    private final PostRepositoryPort postRepository;
    private final MediaStoragePort mediaStorage;
    private final TenantAccessPort tenantAccess;
    private final PostEventPort eventPort;

    public DeletePostUseCase(
        PostRepositoryPort postRepository,
        MediaStoragePort mediaStorage,
        TenantAccessPort tenantAccess,
        PostEventPort eventPort
    ) {
        this.postRepository = postRepository;
        this.mediaStorage = mediaStorage;
        this.tenantAccess = tenantAccess;
        this.eventPort = eventPort;
    }

    /**
     * Command for deleting a post
     */
    public record DeletePostCommand(PostId postId) {}

    /**
     * Execute the delete post use case
     */
    @Transactional
    public void execute(DeletePostCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post = findPostWithAccess(command.postId(), tenant);

        // Cannot delete published posts
        if (post.getStatus().isFinal() && post.allChannelsPublished()) {
            throw new IllegalStateException("Cannot delete a published post");
        }

        // Delete media files from storage
        post.getMedia().forEach(media -> {
            try {
                mediaStorage.delete(media.storageKey());
            } catch (Exception e) {
                // Log error but continue (don't fail the whole operation)
                System.err.println("Failed to delete media: " + media.storageKey() + " - " + e.getMessage());
            }
        });

        // Delete post
        postRepository.delete(command.postId());

        // Publish event
        eventPort.publishPostDeleted(new PostEventPort.PostDeletedEvent(
            command.postId(),
            Instant.now()
        ));
    }

    private Post findPostWithAccess(PostId postId, TenantAccessPort.TenantContext tenant) {
        Post post;

        if (tenant.isAgency()) {
            post = postRepository.findByIdAndClient(postId, tenant.clientId())
                .orElseThrow(() -> new PostNotFoundException(postId));
        } else {
            post = postRepository.findByIdAndOrganization(postId, tenant.organizationId())
                .orElseThrow(() -> new PostNotFoundException(postId));
        }

        if (!tenantAccess.canAccessPost(postId)) {
            throw new UnauthorizedAccessException(tenant.userId(), "Post", postId.toString());
        }

        return post;
    }
}
