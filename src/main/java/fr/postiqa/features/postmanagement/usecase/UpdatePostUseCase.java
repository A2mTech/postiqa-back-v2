package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.ChannelNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.*;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.domain.vo.Content;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case for updating an existing post.
 * Only allows updating draft or scheduled posts.
 */
@Component
public class UpdatePostUseCase {

    private final PostRepositoryPort postRepository;
    private final ChannelRepositoryPort channelRepository;
    private final TenantAccessPort tenantAccess;

    public UpdatePostUseCase(
        PostRepositoryPort postRepository,
        ChannelRepositoryPort channelRepository,
        TenantAccessPort tenantAccess
    ) {
        this.postRepository = postRepository;
        this.channelRepository = channelRepository;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Command for updating a post
     */
    public record UpdatePostCommand(
        PostId postId,
        String contentText,
        List<ChannelId> channelIds
    ) {}

    /**
     * Execute the update post use case
     */
    @Transactional
    public void execute(UpdatePostCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post = findPostWithAccess(command.postId(), tenant);

        // Update content if provided
        if (command.contentText() != null && !command.contentText().isBlank()) {
            Content newContent = Content.of(command.contentText());
            post.updateContent(newContent);
        }

        // Update channels if provided
        if (command.channelIds() != null && !command.channelIds().isEmpty()) {
            updatePostChannels(post, command.channelIds());
        }

        // Save post
        postRepository.save(post);
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

    private void updatePostChannels(Post post, List<ChannelId> newChannelIds) {
        // Validate new channels
        List<Channel> newChannels = channelRepository.findByIds(newChannelIds);

        if (newChannels.size() != newChannelIds.size()) {
            throw new ChannelNotFoundException("One or more channels not found");
        }

        // Verify access to all new channels
        for (Channel channel : newChannels) {
            if (!tenantAccess.canAccessChannel(channel.getId())) {
                throw new UnauthorizedAccessException("Access denied to channel: " + channel.getId());
            }
            if (!channel.isActive()) {
                throw new IllegalArgumentException("Channel is not active: " + channel.getId());
            }
        }

        // Get current channel IDs
        Set<ChannelId> currentChannelIds = new HashSet<>();
        post.getChannelAssignments().forEach(ca -> currentChannelIds.add(ca.channelId()));

        // Add new channels
        for (ChannelId channelId : newChannelIds) {
            if (!currentChannelIds.contains(channelId)) {
                post.addChannel(channelId);
            }
        }

        // Remove channels not in new list
        Set<ChannelId> newChannelIdSet = new HashSet<>(newChannelIds);
        for (ChannelId currentChannelId : currentChannelIds) {
            if (!newChannelIdSet.contains(currentChannelId)) {
                post.removeChannel(currentChannelId);
            }
        }
    }
}
