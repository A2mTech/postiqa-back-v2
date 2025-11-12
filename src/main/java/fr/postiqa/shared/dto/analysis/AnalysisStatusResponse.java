package fr.postiqa.shared.dto.analysis;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for analysis status queries.
 * Provides workflow execution progress and state information.
 */
public record AnalysisStatusResponse(
    UUID analysisId,
    String workflowInstanceId,
    String status,
    String currentStep,
    int completedSteps,
    int totalSteps,
    Instant startedAt,
    String errorMessage
) {}
