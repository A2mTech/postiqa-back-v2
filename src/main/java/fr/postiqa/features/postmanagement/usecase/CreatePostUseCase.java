package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.ChannelNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.*;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.domain.vo.Content;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Use case for creating a new post.
 * Validates channels, creates post, and publishes event.
 */
@Component
public class CreatePostUseCase {

    private final PostRepositoryPort postRepository;
    private final ChannelRepositoryPort channelRepository;
    private final TenantAccessPort tenantAccess;
    private final PostEventPort eventPort;

    public CreatePostUseCase(
        PostRepositoryPort postRepository,
        ChannelRepositoryPort channelRepository,
        TenantAccessPort tenantAccess,
        PostEventPort eventPort
    ) {
        this.postRepository = postRepository;
        this.channelRepository = channelRepository;
        this.tenantAccess = tenantAccess;
        this.eventPort = eventPort;
    }

    /**
     * Command for creating a post
     */
    public record CreatePostCommand(
        String contentText,
        List<ChannelId> channelIds,
        Instant scheduledFor
    ) {}

    /**
     * Execute the create post use case
     */
    @Transactional
    public PostId execute(CreatePostCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Validate channels exist and belong to tenant
        List<Channel> channels = validateAndGetChannels(command.channelIds());

        // Create content from text
        Content content = Content.of(command.contentText());

        // Create post (draft)
        Post post = Post.createDraft(tenant.userId(), content, command.channelIds());

        // Schedule if requested
        if (command.scheduledFor() != null) {
            post.schedule(command.scheduledFor());
        }

        // Save post
        Post savedPost = postRepository.save(post);

        // Publish event
        if (post.isScheduled()) {
            eventPort.publishPostScheduled(new PostEventPort.PostScheduledEvent(
                savedPost.getId(),
                command.channelIds(),
                command.scheduledFor()
            ));
        } else {
            eventPort.publishPostCreated(new PostEventPort.PostCreatedEvent(
                savedPost.getId(),
                command.channelIds(),
                Instant.now()
            ));
        }

        return savedPost.getId();
    }

    private List<Channel> validateAndGetChannels(List<ChannelId> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            throw new IllegalArgumentException("At least one channel must be selected");
        }

        List<Channel> channels = channelRepository.findByIds(channelIds);

        if (channels.size() != channelIds.size()) {
            throw new ChannelNotFoundException("One or more channels not found");
        }

        // Verify access to all channels
        for (Channel channel : channels) {
            if (!tenantAccess.canAccessChannel(channel.getId())) {
                throw new UnauthorizedAccessException("Access denied to channel: " + channel.getId());
            }
        }

        // Verify all channels are active
        channels.stream()
            .filter(c -> !c.isActive())
            .findFirst()
            .ifPresent(c -> {
                throw new IllegalArgumentException("Channel is not active: " + c.getId());
            });

        return channels;
    }
}
