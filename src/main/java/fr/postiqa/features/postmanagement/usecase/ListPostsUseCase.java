package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.enums.PostStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for listing posts with pagination and filters.
 */
@UseCase(
    value = "ListPosts",
    resourceType = "POST",
    description = "Lists posts with pagination and filters",
    logActivity = false
)
public class ListPostsUseCase implements fr.postiqa.shared.usecase.UseCase<ListPostsUseCase.ListPostsQuery, List<Post>> {

    private final PostRepositoryPort postRepository;
    private final TenantAccessPort tenantAccess;

    public ListPostsUseCase(
        PostRepositoryPort postRepository,
        TenantAccessPort tenantAccess
    ) {
        this.postRepository = postRepository;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Query for listing posts
     */
    public record ListPostsQuery(
        PostStatus status,
        int page,
        int size
    ) {
        public ListPostsQuery {
            if (page < 0) {
                throw new IllegalArgumentException("Page must be >= 0");
            }
            if (size <= 0 || size > 100) {
                throw new IllegalArgumentException("Size must be between 1 and 100");
            }
        }
    }

    /**
     * Execute the list posts use case
     */
    @Transactional(readOnly = true)
    public List<Post> execute(ListPostsQuery query) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // List posts based on tenant type and filters
        if (query.status() != null) {
            if (tenant.isAgency()) {
                return postRepository.findByClientAndStatus(
                    tenant.clientId(),
                    query.status(),
                    query.page(),
                    query.size()
                );
            } else {
                return postRepository.findByOrganizationAndStatus(
                    tenant.organizationId(),
                    query.status(),
                    query.page(),
                    query.size()
                );
            }
        } else {
            if (tenant.isAgency()) {
                return postRepository.findByClient(
                    tenant.clientId(),
                    query.page(),
                    query.size()
                );
            } else {
                return postRepository.findByOrganization(
                    tenant.organizationId(),
                    query.page(),
                    query.size()
                );
            }
        }
    }
}
