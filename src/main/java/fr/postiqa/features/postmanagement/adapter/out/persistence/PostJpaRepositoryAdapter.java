package fr.postiqa.features.postmanagement.adapter.out.persistence;

import fr.postiqa.database.entity.*;
import fr.postiqa.database.repository.*;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.vo.*;
import fr.postiqa.features.postmanagement.infrastructure.mapper.ChannelAssignmentMapper;
import fr.postiqa.features.postmanagement.infrastructure.mapper.MediaMapper;
import fr.postiqa.features.postmanagement.infrastructure.mapper.PostMapper;
import fr.postiqa.shared.enums.PostStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing PostRepositoryPort.
 * Bridges between domain models and JPA entities.
 */
@Repository
public class PostJpaRepositoryAdapter implements PostRepositoryPort {

    private final PostRepository postRepository;
    private final PostChannelRepository postChannelRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PostMapper postMapper;
    private final ChannelAssignmentMapper channelAssignmentMapper;
    private final MediaMapper mediaMapper;

    public PostJpaRepositoryAdapter(
        PostRepository postRepository,
        PostChannelRepository postChannelRepository,
        MediaRepository mediaRepository,
        UserRepository userRepository,
        SocialAccountRepository socialAccountRepository,
        PostMapper postMapper,
        ChannelAssignmentMapper channelAssignmentMapper,
        MediaMapper mediaMapper
    ) {
        this.postRepository = postRepository;
        this.postChannelRepository = postChannelRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.postMapper = postMapper;
        this.channelAssignmentMapper = channelAssignmentMapper;
        this.mediaMapper = mediaMapper;
    }

    @Override
    public Post save(Post post) {
        Optional<PostEntity> existingEntity = postRepository.findById(post.getId().value());

        PostEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing
            entity = existingEntity.get();
            postMapper.updateEntity(entity, post);

            // Update channels
            updatePostChannels(entity, post);

            // Update media
            updatePostMedia(entity, post);
        } else {
            // Create new
            entity = postMapper.toEntity(post);

            // Set user
            UserEntity user = userRepository.findById(post.getCreatedBy().value())
                .orElseThrow(() -> new IllegalStateException("User not found: " + post.getCreatedBy()));
            entity.setCreatedBy(user);

            // Save post first to get ID
            entity = postRepository.save(entity);

            // Create channels
            createPostChannels(entity, post);

            // Create media
            createPostMedia(entity, post);
        }

        PostEntity saved = postRepository.save(entity);
        return postMapper.toDomain(saved);
    }

    @Override
    public Optional<Post> findById(PostId postId) {
        return postRepository.findById(postId.value())
            .map(postMapper::toDomain);
    }

    @Override
    public Optional<Post> findByIdAndOrganization(PostId postId, OrganizationId organizationId) {
        boolean exists = postRepository.existsByIdAndOrganizationId(postId.value(), organizationId.value());
        if (!exists) {
            return Optional.empty();
        }
        return findById(postId);
    }

    @Override
    public Optional<Post> findByIdAndClient(PostId postId, ClientId clientId) {
        boolean exists = postRepository.existsByIdAndClientId(postId.value(), clientId.value());
        if (!exists) {
            return Optional.empty();
        }
        return findById(postId);
    }

    @Override
    public List<Post> findByOrganization(OrganizationId organizationId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findPostsByOrganization(organizationId.value(), pageRequest)
            .stream()
            .map(postMapper::toDomain)
            .toList();
    }

    @Override
    public List<Post> findByClient(ClientId clientId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findPostsByClient(clientId.value(), pageRequest)
            .stream()
            .map(postMapper::toDomain)
            .toList();
    }

    @Override
    public List<Post> findByOrganizationAndStatus(OrganizationId organizationId, PostStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findPostsByOrganizationAndStatus(organizationId.value(), status, pageRequest)
            .stream()
            .map(postMapper::toDomain)
            .toList();
    }

    @Override
    public List<Post> findByClientAndStatus(ClientId clientId, PostStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findPostsByClientAndStatus(clientId.value(), status, pageRequest)
            .stream()
            .map(postMapper::toDomain)
            .toList();
    }

    @Override
    public List<Post> findScheduledPostsReadyForPublishing(Instant now) {
        return postRepository.findScheduledPostsReadyForPublishing(now)
            .stream()
            .map(postMapper::toDomain)
            .toList();
    }

    @Override
    public void delete(PostId postId) {
        postRepository.deleteById(postId.value());
    }

    @Override
    public boolean existsByIdAndOrganization(PostId postId, OrganizationId organizationId) {
        return postRepository.existsByIdAndOrganizationId(postId.value(), organizationId.value());
    }

    @Override
    public boolean existsByIdAndClient(PostId postId, ClientId clientId) {
        return postRepository.existsByIdAndClientId(postId.value(), clientId.value());
    }

    // Private helper methods

    private void createPostChannels(PostEntity postEntity, Post post) {
        for (var assignment : post.getChannelAssignments()) {
            PostChannelEntity channelEntity = channelAssignmentMapper.toEntity(assignment);
            channelEntity.setPost(postEntity);

            SocialAccountEntity socialAccount = socialAccountRepository.findById(assignment.channelId().value())
                .orElseThrow(() -> new IllegalStateException("Channel not found: " + assignment.channelId()));
            channelEntity.setChannel(socialAccount);

            postChannelRepository.save(channelEntity);
        }
    }

    private void updatePostChannels(PostEntity postEntity, Post post) {
        // Get existing channels
        List<PostChannelEntity> existingChannels = postChannelRepository.findByPostId(postEntity.getId());

        // Delete channels not in domain model
        for (PostChannelEntity existing : existingChannels) {
            boolean stillExists = post.getChannelAssignments().stream()
                .anyMatch(a -> a.channelId().value().equals(existing.getChannel().getId()));
            if (!stillExists) {
                postChannelRepository.delete(existing);
            }
        }

        // Update or create channels from domain model
        for (var assignment : post.getChannelAssignments()) {
            Optional<PostChannelEntity> existingOpt = existingChannels.stream()
                .filter(e -> e.getChannel().getId().equals(assignment.channelId().value()))
                .findFirst();

            if (existingOpt.isPresent()) {
                // Update existing
                channelAssignmentMapper.updateEntity(existingOpt.get(), assignment);
                postChannelRepository.save(existingOpt.get());
            } else {
                // Create new
                PostChannelEntity channelEntity = channelAssignmentMapper.toEntity(assignment);
                channelEntity.setPost(postEntity);

                SocialAccountEntity socialAccount = socialAccountRepository.findById(assignment.channelId().value())
                    .orElseThrow(() -> new IllegalStateException("Channel not found: " + assignment.channelId()));
                channelEntity.setChannel(socialAccount);

                postChannelRepository.save(channelEntity);
            }
        }
    }

    private void createPostMedia(PostEntity postEntity, Post post) {
        for (var media : post.getMedia()) {
            MediaEntity mediaEntity = mediaMapper.toEntity(media);
            mediaEntity.setPost(postEntity);
            mediaRepository.save(mediaEntity);
        }
    }

    private void updatePostMedia(PostEntity postEntity, Post post) {
        // Get existing media
        List<MediaEntity> existingMedia = mediaRepository.findByPostId(postEntity.getId());

        // Delete media not in domain model
        for (MediaEntity existing : existingMedia) {
            boolean stillExists = post.getMedia().stream()
                .anyMatch(m -> m.id().value().equals(existing.getId()));
            if (!stillExists) {
                mediaRepository.delete(existing);
            }
        }

        // Create new media from domain model
        for (var media : post.getMedia()) {
            boolean isNew = existingMedia.stream()
                .noneMatch(e -> e.getId().equals(media.id().value()));
            if (isNew) {
                MediaEntity mediaEntity = mediaMapper.toEntity(media);
                mediaEntity.setPost(postEntity);
                mediaRepository.save(mediaEntity);
            }
        }
    }
}
