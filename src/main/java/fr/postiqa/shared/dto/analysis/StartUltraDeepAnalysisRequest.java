package fr.postiqa.shared.dto.analysis;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for starting an ultra-deep profile analysis.
 * Contains client information and social media platform URLs.
 */
public record StartUltraDeepAnalysisRequest(
    @NotNull(message = "Client ID is required")
    UUID clientId,

    @NotEmpty(message = "At least one platform must be specified")
    List<String> platforms,

    String websiteUrl,
    String linkedinProfileUrl,
    String twitterProfileUrl,
    String instagramProfileUrl,
    String youtubeProfileUrl,
    String tiktokProfileUrl
) {}
