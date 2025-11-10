package fr.postiqa.shared.dto.postmanagement;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating an existing post.
 */
public record UpdatePostRequest(
    @Size(max = 5000, message = "Content cannot exceed 5000 characters")
    String content,

    List<String> channelIds
) {}
