package fr.postiqa.shared.dto.analysis;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for retrieving the final completed analysis profile.
 * Contains the comprehensive profile data and scoring insights.
 */
public record FinalProfileResponse(
    UUID analysisId,
    Map<String, Object> finalProfile,
    Map<String, Object> scoring,
    Instant completedAt
) {}
