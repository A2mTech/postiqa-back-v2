package fr.postiqa.shared.dto.postmanagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for creating a new post.
 */
public record CreatePostRequest(
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 5000, message = "Content cannot exceed 5000 characters")
    String content,

    @NotEmpty(message = "At least one channel must be selected")
    List<String> channelIds,

    Instant scheduledFor
) {}
