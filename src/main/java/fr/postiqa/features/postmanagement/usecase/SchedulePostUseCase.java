package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.PostEventPort;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.ChannelAssignment;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use case for scheduling a post for future publishing.
 */
@Component
public class SchedulePostUseCase {

    private final PostRepositoryPort postRepository;
    private final TenantAccessPort tenantAccess;
    private final PostEventPort eventPort;

    public SchedulePostUseCase(
        PostRepositoryPort postRepository,
        TenantAccessPort tenantAccess,
        PostEventPort eventPort
    ) {
        this.postRepository = postRepository;
        this.tenantAccess = tenantAccess;
        this.eventPort = eventPort;
    }

    /**
     * Command for scheduling a post
     */
    public record SchedulePostCommand(
        PostId postId,
        Instant scheduledFor
    ) {
        public SchedulePostCommand {
            if (scheduledFor == null) {
                throw new IllegalArgumentException("Scheduled time cannot be null");
            }
            if (scheduledFor.isBefore(Instant.now())) {
                throw new IllegalArgumentException("Scheduled time cannot be in the past");
            }
        }
    }

    /**
     * Execute the schedule post use case
     */
    @Transactional
    public void execute(SchedulePostCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post = findPostWithAccess(command.postId(), tenant);

        // Schedule post
        post.schedule(command.scheduledFor());

        // Save post
        postRepository.save(post);

        // Publish event
        eventPort.publishPostScheduled(new PostEventPort.PostScheduledEvent(
            post.getId(),
            post.getChannelAssignments().stream().map(ChannelAssignment::channelId).toList(),
            command.scheduledFor()
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
