package fr.postiqa.shared.dto.postmanagement;

/**
 * Response DTO for media upload.
 */
public record UploadMediaResponse(
    String mediaId,
    String url,
    String fileName,
    String type,
    long fileSize
) {}
