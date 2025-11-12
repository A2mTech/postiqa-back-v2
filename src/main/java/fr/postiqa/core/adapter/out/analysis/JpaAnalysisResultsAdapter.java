package fr.postiqa.core.adapter.out.analysis;

import fr.postiqa.database.entity.*;
import fr.postiqa.database.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * JPA adapter for persisting ultra-deep analysis results.
 * <p>
 * Maps domain analysis results to JPA entities and persists them.
 * Provides methods for saving all analysis phases: site, profiles, posts, summaries, cross-ref, scoring.
 */
@Component
public class JpaAnalysisResultsAdapter {

    private static final Logger log = LoggerFactory.getLogger(JpaAnalysisResultsAdapter.class);

    private final UserProfileAnalysisRepository userProfileAnalysisRepository;
    private final SiteAnalysisRepository siteAnalysisRepository;
    private final PlatformProfileAnalysisRepository platformProfileAnalysisRepository;
    private final PostAnalysisRepository postAnalysisRepository;
    private final PlatformSummaryRepository platformSummaryRepository;
    private final CrossReferenceAnalysisRepository crossReferenceAnalysisRepository;
    private final ScoringInsightsRepository scoringInsightsRepository;

    public JpaAnalysisResultsAdapter(
        UserProfileAnalysisRepository userProfileAnalysisRepository,
        SiteAnalysisRepository siteAnalysisRepository,
        PlatformProfileAnalysisRepository platformProfileAnalysisRepository,
        PostAnalysisRepository postAnalysisRepository,
        PlatformSummaryRepository platformSummaryRepository,
        CrossReferenceAnalysisRepository crossReferenceAnalysisRepository,
        ScoringInsightsRepository scoringInsightsRepository
    ) {
        this.userProfileAnalysisRepository = userProfileAnalysisRepository;
        this.siteAnalysisRepository = siteAnalysisRepository;
        this.platformProfileAnalysisRepository = platformProfileAnalysisRepository;
        this.postAnalysisRepository = postAnalysisRepository;
        this.platformSummaryRepository = platformSummaryRepository;
        this.crossReferenceAnalysisRepository = crossReferenceAnalysisRepository;
        this.scoringInsightsRepository = scoringInsightsRepository;
    }

    /**
     * Save site analysis result
     */
    @Transactional
    public UUID saveSiteAnalysis(UUID userProfileAnalysisId, SiteAnalysisEntity siteAnalysis) {
        log.info("Saving site analysis for profile analysis: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        siteAnalysis.setUserProfileAnalysis(userProfileAnalysis);
        SiteAnalysisEntity saved = siteAnalysisRepository.save(siteAnalysis);

        return saved.getId();
    }

    /**
     * Save platform profile analysis result
     */
    @Transactional
    public UUID savePlatformProfileAnalysis(UUID userProfileAnalysisId, PlatformProfileAnalysisEntity platformProfile) {
        log.info("Saving platform profile analysis for profile analysis: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        platformProfile.setUserProfileAnalysis(userProfileAnalysis);
        PlatformProfileAnalysisEntity saved = platformProfileAnalysisRepository.save(platformProfile);

        return saved.getId();
    }

    /**
     * Save post analysis result
     */
    @Transactional
    public UUID savePostAnalysis(UUID platformProfileId, PostAnalysisEntity postAnalysis) {
        log.debug("Saving post analysis for platform profile: {}", platformProfileId);

        PlatformProfileAnalysisEntity platformProfile = platformProfileAnalysisRepository.findById(platformProfileId)
            .orElseThrow(() -> new IllegalArgumentException("PlatformProfile not found: " + platformProfileId));

        postAnalysis.setPlatformProfile(platformProfile);
        PostAnalysisEntity saved = postAnalysisRepository.save(postAnalysis);

        return saved.getId();
    }

    /**
     * Save platform summary result
     */
    @Transactional
    public UUID savePlatformSummary(UUID userProfileAnalysisId, PlatformSummaryEntity platformSummary) {
        log.info("Saving platform summary for profile analysis: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        platformSummary.setUserProfileAnalysis(userProfileAnalysis);
        PlatformSummaryEntity saved = platformSummaryRepository.save(platformSummary);

        return saved.getId();
    }

    /**
     * Save cross-reference analysis result
     */
    @Transactional
    public UUID saveCrossReferenceAnalysis(UUID userProfileAnalysisId, CrossReferenceAnalysisEntity crossReference) {
        log.info("Saving cross-reference analysis for profile analysis: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        crossReference.setUserProfileAnalysis(userProfileAnalysis);
        CrossReferenceAnalysisEntity saved = crossReferenceAnalysisRepository.save(crossReference);

        return saved.getId();
    }

    /**
     * Save scoring insights result
     */
    @Transactional
    public UUID saveScoringInsights(UUID userProfileAnalysisId, ScoringInsightsEntity scoringInsights) {
        log.info("Saving scoring insights for profile analysis: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        scoringInsights.setUserProfileAnalysis(userProfileAnalysis);
        ScoringInsightsEntity saved = scoringInsightsRepository.save(scoringInsights);

        return saved.getId();
    }

    /**
     * Update user profile analysis with final profile
     */
    @Transactional
    public void updateFinalProfile(UUID userProfileAnalysisId, java.util.Map<String, Object> finalProfile) {
        log.info("Updating final profile for profile analysis: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        userProfileAnalysis.setFinalProfile(finalProfile);
        userProfileAnalysisRepository.save(userProfileAnalysis);
    }

    /**
     * Update analysis status
     */
    @Transactional
    public void updateAnalysisStatus(UUID userProfileAnalysisId, String status) {
        log.info("Updating analysis status to '{}' for: {}", status, userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        userProfileAnalysis.setStatus(status);

        if ("RUNNING".equals(status) && userProfileAnalysis.getStartedAt() == null) {
            userProfileAnalysis.setStartedAt(java.time.Instant.now());
        } else if ("COMPLETED".equals(status) && userProfileAnalysis.getCompletedAt() == null) {
            userProfileAnalysis.setCompletedAt(java.time.Instant.now());
        }

        userProfileAnalysisRepository.save(userProfileAnalysis);
    }

    /**
     * Mark analysis as failed
     */
    @Transactional
    public void markAnalysisFailed(UUID userProfileAnalysisId, String errorMessage) {
        log.error("Marking analysis as failed: {}", userProfileAnalysisId);

        UserProfileAnalysisEntity userProfileAnalysis = userProfileAnalysisRepository.findById(userProfileAnalysisId)
            .orElseThrow(() -> new IllegalArgumentException("UserProfileAnalysis not found: " + userProfileAnalysisId));

        userProfileAnalysis.setStatus("FAILED");
        userProfileAnalysis.setErrorMessage(errorMessage);
        userProfileAnalysis.setCompletedAt(java.time.Instant.now());

        userProfileAnalysisRepository.save(userProfileAnalysis);
    }
}
