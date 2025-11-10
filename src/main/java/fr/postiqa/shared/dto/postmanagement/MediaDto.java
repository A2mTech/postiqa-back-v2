package fr.postiqa.shared.dto.postmanagement;

import fr.postiqa.shared.enums.MediaType;

/**
 * Response DTO for media.
 */
public record MediaDto(
    String id,
    String url,
    String fileName,
    MediaType type,
    long fileSize,
    Integer width,
    Integer height,
    Integer durationSeconds
) {}
