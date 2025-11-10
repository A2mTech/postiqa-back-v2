package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.MediaNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.MediaStoragePort;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.Media;
import fr.postiqa.features.postmanagement.domain.vo.MediaId;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for deleting media from a post.
 */
@Component
public class DeleteMediaUseCase {

    private final PostRepositoryPort postRepository;
    private final MediaStoragePort mediaStorage;
    private final TenantAccessPort tenantAccess;

    public DeleteMediaUseCase(
        PostRepositoryPort postRepository,
        MediaStoragePort mediaStorage,
        TenantAccessPort tenantAccess
    ) {
        this.postRepository = postRepository;
        this.mediaStorage = mediaStorage;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Command for deleting media
     */
    public record DeleteMediaCommand(
        PostId postId,
        MediaId mediaId
    ) {}

    /**
     * Execute the delete media use case
     */
    @Transactional
    public void execute(DeleteMediaCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post = findPostWithAccess(command.postId(), tenant);

        // Find media in post
        Media media = post.getMedia().stream()
            .filter(m -> m.id().equals(command.mediaId()))
            .findFirst()
            .orElseThrow(() -> new MediaNotFoundException(command.mediaId()));

        // Remove media from post
        post.removeMedia(command.mediaId());

        // Save post
        postRepository.save(post);

        // Delete from storage (best effort)
        try {
            mediaStorage.delete(media.storageKey());
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to delete media from storage: " + media.storageKey() + " - " + e.getMessage());
        }
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
