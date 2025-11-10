package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a single post by ID.
 */
@Component
public class GetPostUseCase {

    private final PostRepositoryPort postRepository;
    private final TenantAccessPort tenantAccess;

    public GetPostUseCase(
        PostRepositoryPort postRepository,
        TenantAccessPort tenantAccess
    ) {
        this.postRepository = postRepository;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Query for getting a post
     */
    public record GetPostQuery(PostId postId) {}

    /**
     * Execute the get post use case
     */
    @Transactional(readOnly = true)
    public Post execute(GetPostQuery query) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post;

        if (tenant.isAgency()) {
            post = postRepository.findByIdAndClient(query.postId(), tenant.clientId())
                .orElseThrow(() -> new PostNotFoundException(query.postId()));
        } else {
            post = postRepository.findByIdAndOrganization(query.postId(), tenant.organizationId())
                .orElseThrow(() -> new PostNotFoundException(query.postId()));
        }

        if (!tenantAccess.canAccessPost(query.postId())) {
            throw new UnauthorizedAccessException(tenant.userId(), "Post", query.postId().toString());
        }

        return post;
    }
}
