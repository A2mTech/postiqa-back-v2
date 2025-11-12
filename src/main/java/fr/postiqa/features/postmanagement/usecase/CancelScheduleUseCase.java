package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.PostEventPort;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import fr.postiqa.shared.annotation.UseCase;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use case for cancelling a scheduled post.
 */
@UseCase(
    value = "CancelSchedule",
    resourceType = "POST",
    description = "Cancels a scheduled post"
)
public class CancelScheduleUseCase implements fr.postiqa.shared.usecase.UseCase<CancelScheduleUseCase.CancelScheduleCommand, Void> {

    private final PostRepositoryPort postRepository;
    private final TenantAccessPort tenantAccess;
    private final PostEventPort eventPort;

    public CancelScheduleUseCase(
        PostRepositoryPort postRepository,
        TenantAccessPort tenantAccess,
        PostEventPort eventPort
    ) {
        this.postRepository = postRepository;
        this.tenantAccess = tenantAccess;
        this.eventPort = eventPort;
    }

    /**
     * Command for cancelling a scheduled post
     */
    public record CancelScheduleCommand(PostId postId) {}

    /**
     * Execute the cancel schedule use case
     */
    @Transactional
    public Void execute(CancelScheduleCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post = findPostWithAccess(command.postId(), tenant);

        // Cancel schedule
        post.cancelSchedule();

        // Save post
        postRepository.save(post);

        // Publish event
        eventPort.publishPostScheduleCancelled(new PostEventPort.PostScheduleCancelledEvent(
            post.getId(),
            Instant.now()
        ));

        return null;
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
