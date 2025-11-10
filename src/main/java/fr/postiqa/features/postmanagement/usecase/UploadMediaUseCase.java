package fr.postiqa.features.postmanagement.usecase;

import fr.postiqa.features.postmanagement.domain.exception.MediaUploadException;
import fr.postiqa.features.postmanagement.domain.exception.PostNotFoundException;
import fr.postiqa.features.postmanagement.domain.exception.UnauthorizedAccessException;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.port.MediaStoragePort;
import fr.postiqa.features.postmanagement.domain.port.PostRepositoryPort;
import fr.postiqa.features.postmanagement.domain.port.TenantAccessPort;
import fr.postiqa.features.postmanagement.domain.vo.Media;
import fr.postiqa.features.postmanagement.domain.vo.MediaId;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import fr.postiqa.shared.enums.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for uploading media to a post.
 */
@Component
public class UploadMediaUseCase {

    private final PostRepositoryPort postRepository;
    private final MediaStoragePort mediaStorage;
    private final TenantAccessPort tenantAccess;

    public UploadMediaUseCase(
        PostRepositoryPort postRepository,
        MediaStoragePort mediaStorage,
        TenantAccessPort tenantAccess
    ) {
        this.postRepository = postRepository;
        this.mediaStorage = mediaStorage;
        this.tenantAccess = tenantAccess;
    }

    /**
     * Command for uploading media
     */
    public record UploadMediaCommand(
        PostId postId,
        byte[] fileBytes,
        String fileName,
        String mimeType,
        long fileSize
    ) {
        public UploadMediaCommand {
            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalArgumentException("File bytes cannot be null or empty");
            }
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("File name cannot be null or blank");
            }
            if (mimeType == null || mimeType.isBlank()) {
                throw new IllegalArgumentException("MIME type cannot be null or blank");
            }
            if (fileSize <= 0) {
                throw new IllegalArgumentException("File size must be positive");
            }
        }
    }

    /**
     * Execute the upload media use case
     */
    @Transactional
    public Media execute(UploadMediaCommand command) {
        // Get current tenant context
        TenantAccessPort.TenantContext tenant = tenantAccess.getCurrentTenant();

        // Find post with authorization check
        Post post = findPostWithAccess(command.postId(), tenant);

        // Validate media type
        MediaType mediaType = MediaType.fromMimeType(command.mimeType());
        if (mediaType == null) {
            throw new MediaUploadException(command.fileName(), "Unsupported MIME type: " + command.mimeType());
        }

        // Validate file size
        if (!mediaType.isValidSize(command.fileSize())) {
            throw new MediaUploadException(
                command.fileName(),
                "File size " + command.fileSize() + " exceeds maximum " + mediaType.getMaxSizeBytes() + " bytes"
            );
        }

        // Generate media ID
        MediaId mediaId = MediaId.generate();

        // Upload to storage
        MediaStoragePort.UploadContext context = new MediaStoragePort.UploadContext(
            tenant.organizationId(),
            mediaId,
            command.fileName()
        );

        MediaStoragePort.UploadResult uploadResult;
        try {
            uploadResult = mediaStorage.upload(command.fileBytes(), context, command.mimeType());
        } catch (Exception e) {
            throw new MediaUploadException(command.fileName(), "Upload failed", e);
        }

        // Create media value object
        Media media = Media.create(
            mediaId,
            uploadResult.storageKey(),
            uploadResult.publicUrl(),
            command.fileName(),
            command.mimeType(),
            mediaType,
            command.fileSize(),
            null, // width - can be extracted from image metadata in future
            null, // height
            null  // duration
        );

        // Add media to post
        post.addMedia(media);

        // Save post
        postRepository.save(post);

        return media;
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
