package fr.postiqa.shared.dto.analysis;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for ultra-deep analysis.
 * Contains analysis metadata and final profile data.
 */
public record UltraDeepAnalysisResponse(
    UUID analysisId,
    String workflowInstanceId,
    String status,
    List<String> platforms,
    Instant startedAt,
    Instant completedAt,
    Map<String, Object> finalProfile
) {}
