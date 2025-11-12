package fr.postiqa.agency.controller;

import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.usecase.StartUltraDeepAnalysisUseCase;
import fr.postiqa.core.usecase.workflow.GetWorkflowStatusUseCase;
import fr.postiqa.database.entity.UserProfileAnalysisEntity;
import fr.postiqa.database.repository.UserProfileAnalysisRepository;
import fr.postiqa.shared.dto.analysis.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for agency profile analysis management.
 * Handles ultra-deep profile analysis operations for agency clients.
 */
@RestController
@RequestMapping("/api/agency/analysis")
@RequiredArgsConstructor
@Slf4j
public class AgencyAnalysisController {

    private final StartUltraDeepAnalysisUseCase startAnalysisUseCase;
    private final UserProfileAnalysisRepository analysisRepository;
    private final GetWorkflowStatusUseCase getWorkflowStatusUseCase;

    /**
     * Start a new ultra-deep profile analysis.
     * POST /api/agency/analysis/ultra-deep
     *
     * @param request Request containing client ID and platform URLs
     * @param organizationId Organization ID from header
     * @return Analysis response with workflow instance ID
     */
    @PostMapping("/ultra-deep")
    public ResponseEntity<UltraDeepAnalysisResponse> startUltraDeepAnalysis(
        @Valid @RequestBody StartUltraDeepAnalysisRequest request,
        @RequestHeader("X-Organization-Id") UUID organizationId
    ) {
        log.info("Starting ultra-deep analysis for organization {} and client {}",
            organizationId, request.clientId());

        // Build command
        var command = new StartUltraDeepAnalysisUseCase.StartUltraDeepAnalysisCommand(
            organizationId,
            request.clientId(),
            request.platforms(),
            request.websiteUrl(),
            request.linkedinProfileUrl(),
            request.twitterProfileUrl(),
            request.instagramProfileUrl(),
            request.youtubeProfileUrl(),
            request.tiktokProfileUrl()
        );

        // Start analysis workflow
        String workflowInstanceId = startAnalysisUseCase.execute(command);

        // Retrieve the created analysis entity
        UserProfileAnalysisEntity analysis = analysisRepository
            .findByWorkflowInstanceId(workflowInstanceId)
            .orElseThrow(() -> new IllegalStateException(
                "Analysis entity not found after creation"));

        log.info("Ultra-deep analysis started successfully. Analysis ID: {}, Workflow ID: {}",
            analysis.getId(), workflowInstanceId);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapToResponse(analysis));
    }

    /**
     * Get analysis by ID.
     * GET /api/agency/analysis/{analysisId}
     *
     * @param analysisId Analysis UUID
     * @param organizationId Organization ID from header
     * @return Analysis response
     */
    @GetMapping("/{analysisId}")
    public ResponseEntity<UltraDeepAnalysisResponse> getAnalysis(
        @PathVariable UUID analysisId,
        @RequestHeader("X-Organization-Id") UUID organizationId
    ) {
        log.debug("Retrieving analysis {} for organization {}", analysisId, organizationId);

        // Find analysis and validate organization scope
        UserProfileAnalysisEntity analysis = analysisRepository
            .findByIdAndOrganizationId(analysisId, organizationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Analysis not found or access denied"));

        return ResponseEntity.ok(mapToResponse(analysis));
    }

    /**
     * Get analysis status with workflow progress.
     * GET /api/agency/analysis/{analysisId}/status
     *
     * @param analysisId Analysis UUID
     * @param organizationId Organization ID from header
     * @return Analysis status with workflow execution details
     */
    @GetMapping("/{analysisId}/status")
    public ResponseEntity<AnalysisStatusResponse> getAnalysisStatus(
        @PathVariable UUID analysisId,
        @RequestHeader("X-Organization-Id") UUID organizationId
    ) {
        log.debug("Retrieving status for analysis {} in organization {}", analysisId, organizationId);

        // Find analysis and validate organization scope
        UserProfileAnalysisEntity analysis = analysisRepository
            .findByIdAndOrganizationId(analysisId, organizationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Analysis not found or access denied"));

        // Get workflow status
        WorkflowInstance workflowInstance = getWorkflowStatusUseCase
            .execute(analysis.getWorkflowInstanceId())
            .orElse(null);

        // Build status response
        String currentStepId = null;
        int completedSteps = 0;

        if (workflowInstance != null) {
            // Get current step: find the last executing or recently completed step
            currentStepId = workflowInstance.getStepExecutions().entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().startedAt().compareTo(e1.getValue().startedAt()))
                .findFirst()
                .map(entry -> entry.getKey())
                .orElse(null);

            // Get completed steps count
            completedSteps = workflowInstance.getCompletedStepIds().size();
        }

        AnalysisStatusResponse statusResponse = new AnalysisStatusResponse(
            analysis.getId(),
            analysis.getWorkflowInstanceId(),
            analysis.getStatus(),
            currentStepId,
            completedSteps,
            24, // Total steps in ultra-deep analysis workflow
            analysis.getStartedAt(),
            analysis.getErrorMessage()
        );

        return ResponseEntity.ok(statusResponse);
    }

    /**
     * Get final profile after analysis completion.
     * GET /api/agency/analysis/{analysisId}/final-profile
     *
     * @param analysisId Analysis UUID
     * @param organizationId Organization ID from header
     * @return Final profile response with scoring
     */
    @GetMapping("/{analysisId}/final-profile")
    public ResponseEntity<FinalProfileResponse> getFinalProfile(
        @PathVariable UUID analysisId,
        @RequestHeader("X-Organization-Id") UUID organizationId
    ) {
        log.debug("Retrieving final profile for analysis {} in organization {}",
            analysisId, organizationId);

        // Find analysis and validate organization scope
        UserProfileAnalysisEntity analysis = analysisRepository
            .findByIdAndOrganizationId(analysisId, organizationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Analysis not found or access denied"));

        // Validate analysis is completed
        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new IllegalStateException(
                "Analysis is not yet completed. Current status: " + analysis.getStatus());
        }

        // Extract scoring from final profile (if available)
        Map<String, Object> scoring = new HashMap<>();
        if (analysis.getFinalProfile() != null && analysis.getFinalProfile().containsKey("scoring")) {
            scoring = (Map<String, Object>) analysis.getFinalProfile().get("scoring");
        }

        FinalProfileResponse response = new FinalProfileResponse(
            analysis.getId(),
            analysis.getFinalProfile(),
            scoring,
            analysis.getCompletedAt()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * List all analyses for the organization.
     * GET /api/agency/analysis
     *
     * @param organizationId Organization ID from header
     * @param status Optional status filter
     * @param clientId Optional client ID filter
     * @return List of analysis responses
     */
    @GetMapping
    public ResponseEntity<java.util.List<UltraDeepAnalysisResponse>> listAnalyses(
        @RequestHeader("X-Organization-Id") UUID organizationId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) UUID clientId
    ) {
        log.debug("Listing analyses for organization {} (status: {}, client: {})",
            organizationId, status, clientId);

        java.util.List<UserProfileAnalysisEntity> analyses;

        if (clientId != null && status != null) {
            analyses = analysisRepository.findByClientIdAndStatus(clientId, status);
        } else if (clientId != null) {
            analyses = analysisRepository.findByClientId(clientId);
        } else if (status != null) {
            analyses = analysisRepository.findByOrganizationIdAndStatus(organizationId, status);
        } else {
            analyses = analysisRepository.findByOrganizationId(organizationId);
        }

        java.util.List<UltraDeepAnalysisResponse> responses = analyses.stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Map entity to response DTO
     */
    private UltraDeepAnalysisResponse mapToResponse(UserProfileAnalysisEntity entity) {
        return new UltraDeepAnalysisResponse(
            entity.getId(),
            entity.getWorkflowInstanceId(),
            entity.getStatus(),
            entity.getPlatforms(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getFinalProfile()
        );
    }
}
