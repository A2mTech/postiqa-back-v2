package fr.postiqa.core.infrastructure.workflow.ultradeep;

import fr.postiqa.core.domain.workflow.enums.CompensationStrategy;
import fr.postiqa.core.domain.workflow.enums.ExecutionMode;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.infrastructure.workflow.ultradeep.step.aggregation.*;
import fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis.*;
import fr.postiqa.core.infrastructure.workflow.ultradeep.step.finalization.*;
import fr.postiqa.core.infrastructure.workflow.ultradeep.step.scraping.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Factory for creating the Ultra-Deep Analysis Workflow.
 * <p>
 * This workflow orchestrates 24 steps across 4 phases:
 * <ul>
 *   <li>Phase 1: SCRAPING (11 steps) - Parallel scraping of website + 5 social platforms</li>
 *   <li>Phase 2: ANALYSIS (9 steps) - Parallel analysis of site, profiles, and posts</li>
 *   <li>Phase 3: SYNTHESIS (3 steps) - Platform summaries, cross-reference, scoring</li>
 *   <li>Phase 4: FINAL (1 step) - Generate comprehensive final profile</li>
 * </ul>
 * <p>
 * Total duration: ~3 hours (video analysis is the bottleneck)
 * Execution mode: PARALLEL - maximizes throughput with proper dependencies
 */
@Slf4j
@Component
public class UltraDeepAnalysisWorkflowFactory {

    // ==================== PHASE 1: SCRAPING STEPS (11) ====================
    private final ScrapeWebsiteStep scrapeWebsiteStep;
    private final ScrapeLinkedInProfileStep scrapeLinkedInProfileStep;
    private final ScrapeLinkedInPostsStep scrapeLinkedInPostsStep;
    private final ScrapeTwitterProfileStep scrapeTwitterProfileStep;
    private final ScrapeTwitterPostsStep scrapeTwitterPostsStep;
    private final ScrapeInstagramProfileStep scrapeInstagramProfileStep;
    private final ScrapeInstagramPostsStep scrapeInstagramPostsStep;
    private final ScrapeYouTubeProfileStep scrapeYouTubeProfileStep;
    private final ScrapeYouTubePostsStep scrapeYouTubePostsStep;
    private final ScrapeTikTokProfileStep scrapeTikTokProfileStep;
    private final ScrapeTikTokPostsStep scrapeTikTokPostsStep;

    // ==================== PHASE 2: ANALYSIS STEPS (9) ====================
    private final AnalyzeSiteStep analyzeSiteStep;
    private final AnalyzeProfilePictureStep analyzeProfilePictureStep;
    private final AnalyzeBannerStep analyzeBannerStep;
    private final AnalyzeBioStep analyzeBioStep;
    private final AnalyzeTextPostsStep analyzeTextPostsStep;
    private final AnalyzeImagePostsStep analyzeImagePostsStep;
    private final AnalyzeCarouselPostsStep analyzeCarouselPostsStep;
    private final AnalyzeVideoPostsStep analyzeVideoPostsStep;
    private final AnalyzeThreadPostsStep analyzeThreadPostsStep;

    // ==================== PHASE 3: SYNTHESIS STEPS (3) ====================
    private final GeneratePlatformSummaryStep generatePlatformSummaryStep;
    private final CrossReferenceAnalysisStep crossReferenceAnalysisStep;
    private final GenerateScoringInsightsStep generateScoringInsightsStep;

    // ==================== PHASE 4: FINAL STEP (1) ====================
    private final GenerateFinalProfileStep generateFinalProfileStep;

    /**
     * Constructor with dependency injection of all workflow steps
     */
    public UltraDeepAnalysisWorkflowFactory(
        // Phase 1: Scraping
        ScrapeWebsiteStep scrapeWebsiteStep,
        ScrapeLinkedInProfileStep scrapeLinkedInProfileStep,
        ScrapeLinkedInPostsStep scrapeLinkedInPostsStep,
        ScrapeTwitterProfileStep scrapeTwitterProfileStep,
        ScrapeTwitterPostsStep scrapeTwitterPostsStep,
        ScrapeInstagramProfileStep scrapeInstagramProfileStep,
        ScrapeInstagramPostsStep scrapeInstagramPostsStep,
        ScrapeYouTubeProfileStep scrapeYouTubeProfileStep,
        ScrapeYouTubePostsStep scrapeYouTubePostsStep,
        ScrapeTikTokProfileStep scrapeTikTokProfileStep,
        ScrapeTikTokPostsStep scrapeTikTokPostsStep,
        // Phase 2: Analysis
        AnalyzeSiteStep analyzeSiteStep,
        AnalyzeProfilePictureStep analyzeProfilePictureStep,
        AnalyzeBannerStep analyzeBannerStep,
        AnalyzeBioStep analyzeBioStep,
        AnalyzeTextPostsStep analyzeTextPostsStep,
        AnalyzeImagePostsStep analyzeImagePostsStep,
        AnalyzeCarouselPostsStep analyzeCarouselPostsStep,
        AnalyzeVideoPostsStep analyzeVideoPostsStep,
        AnalyzeThreadPostsStep analyzeThreadPostsStep,
        // Phase 3: Synthesis
        GeneratePlatformSummaryStep generatePlatformSummaryStep,
        CrossReferenceAnalysisStep crossReferenceAnalysisStep,
        GenerateScoringInsightsStep generateScoringInsightsStep,
        // Phase 4: Final
        GenerateFinalProfileStep generateFinalProfileStep
    ) {
        // Phase 1: Scraping
        this.scrapeWebsiteStep = scrapeWebsiteStep;
        this.scrapeLinkedInProfileStep = scrapeLinkedInProfileStep;
        this.scrapeLinkedInPostsStep = scrapeLinkedInPostsStep;
        this.scrapeTwitterProfileStep = scrapeTwitterProfileStep;
        this.scrapeTwitterPostsStep = scrapeTwitterPostsStep;
        this.scrapeInstagramProfileStep = scrapeInstagramProfileStep;
        this.scrapeInstagramPostsStep = scrapeInstagramPostsStep;
        this.scrapeYouTubeProfileStep = scrapeYouTubeProfileStep;
        this.scrapeYouTubePostsStep = scrapeYouTubePostsStep;
        this.scrapeTikTokProfileStep = scrapeTikTokProfileStep;
        this.scrapeTikTokPostsStep = scrapeTikTokPostsStep;
        // Phase 2: Analysis
        this.analyzeSiteStep = analyzeSiteStep;
        this.analyzeProfilePictureStep = analyzeProfilePictureStep;
        this.analyzeBannerStep = analyzeBannerStep;
        this.analyzeBioStep = analyzeBioStep;
        this.analyzeTextPostsStep = analyzeTextPostsStep;
        this.analyzeImagePostsStep = analyzeImagePostsStep;
        this.analyzeCarouselPostsStep = analyzeCarouselPostsStep;
        this.analyzeVideoPostsStep = analyzeVideoPostsStep;
        this.analyzeThreadPostsStep = analyzeThreadPostsStep;
        // Phase 3: Synthesis
        this.generatePlatformSummaryStep = generatePlatformSummaryStep;
        this.crossReferenceAnalysisStep = crossReferenceAnalysisStep;
        this.generateScoringInsightsStep = generateScoringInsightsStep;
        // Phase 4: Final
        this.generateFinalProfileStep = generateFinalProfileStep;
    }

    /**
     * Creates the Ultra-Deep Analysis Workflow definition.
     * <p>
     * This workflow uses PARALLEL execution mode to maximize throughput.
     * Dependencies are carefully managed to ensure proper data flow:
     * <ul>
     *   <li>Posts scraping depends on profile scraping (need URLs)</li>
     *   <li>Analysis steps depend on their respective scraping steps</li>
     *   <li>Synthesis steps depend on all analysis steps</li>
     *   <li>Final profile depends on synthesis completion</li>
     * </ul>
     *
     * @return Immutable workflow definition ready for execution
     */
    public WorkflowDefinition createWorkflow() {
        log.info("Building Ultra-Deep Analysis Workflow with all 24 steps");

        return WorkflowDefinition.builder()
            .name("UltraDeepAnalysisWorkflow")
            .description("4-phase ultra-deep user profile analysis: Scrape → Analyze → Synthesize → Generate")
            .executionMode(ExecutionMode.PARALLEL) // Enable parallel execution for maximum speed
            .compensationStrategy(CompensationStrategy.REVERSE_ORDER)
            .globalTimeout(Duration.ofHours(3)) // Videos take a long time

            // ====================================================================
            // PHASE 1: SCRAPING (11 steps)
            // ====================================================================
            // Root step: Scrape website (no dependencies)
            .addStep(scrapeWebsiteStep)

            // Social profiles (no dependencies - all execute in parallel)
            .addStep(scrapeLinkedInProfileStep)
            .addStep(scrapeTwitterProfileStep)
            .addStep(scrapeInstagramProfileStep)
            .addStep(scrapeYouTubeProfileStep)
            .addStep(scrapeTikTokProfileStep)

            // Social posts (depend on their profile scraping)
            .addStep(scrapeLinkedInPostsStep, "scrape-linkedin-profile")
            .addStep(scrapeTwitterPostsStep, "scrape-twitter-profile")
            .addStep(scrapeInstagramPostsStep, "scrape-instagram-profile")
            .addStep(scrapeYouTubePostsStep, "scrape-youtube-profile")
            .addStep(scrapeTikTokPostsStep, "scrape-tiktok-profile")

            // ====================================================================
            // PHASE 2A: SITE ANALYSIS (1 step)
            // ====================================================================
            .addStep(analyzeSiteStep, "scrape-website")

            // ====================================================================
            // PHASE 2B: PROFILE ANALYSIS (3 steps - parallel)
            // ====================================================================
            // These steps analyze profile data from ALL platforms
            .addStep(analyzeProfilePictureStep,
                "scrape-linkedin-profile",
                "scrape-twitter-profile",
                "scrape-instagram-profile",
                "scrape-youtube-profile",
                "scrape-tiktok-profile")

            .addStep(analyzeBannerStep,
                "scrape-linkedin-profile",
                "scrape-twitter-profile",
                "scrape-instagram-profile",
                "scrape-youtube-profile",
                "scrape-tiktok-profile")

            .addStep(analyzeBioStep,
                "scrape-linkedin-profile",
                "scrape-twitter-profile",
                "scrape-instagram-profile",
                "scrape-youtube-profile",
                "scrape-tiktok-profile")

            // ====================================================================
            // PHASE 2C: POSTS ANALYSIS (5 steps)
            // ====================================================================
            // Text posts from all platforms
            .addStep(analyzeTextPostsStep,
                "scrape-linkedin-posts",
                "scrape-twitter-posts",
                "scrape-instagram-posts",
                "scrape-youtube-posts",
                "scrape-tiktok-posts")

            // Image posts from visual platforms
            .addStep(analyzeImagePostsStep,
                "scrape-linkedin-posts",
                "scrape-twitter-posts",
                "scrape-instagram-posts",
                "scrape-youtube-posts",
                "scrape-tiktok-posts")

            // Carousel posts (LinkedIn, Instagram)
            .addStep(analyzeCarouselPostsStep,
                "scrape-linkedin-posts",
                "scrape-instagram-posts")

            // Video posts (YouTube, TikTok, Instagram)
            .addStep(analyzeVideoPostsStep,
                "scrape-youtube-posts",
                "scrape-tiktok-posts",
                "scrape-instagram-posts")

            // Thread posts (Twitter)
            .addStep(analyzeThreadPostsStep,
                "scrape-twitter-posts")

            // ====================================================================
            // PHASE 3: SYNTHESIS (3 steps)
            // ====================================================================
            // Platform-specific summaries
            .addStep(generatePlatformSummaryStep,
                "analyze-profile-pictures",
                "analyze-banners",
                "analyze-bios",
                "analyze-text-posts",
                "analyze-image-posts",
                "analyze-carousel-posts",
                "analyze-video-posts",
                "analyze-thread-posts")

            // Cross-reference analysis
            .addStep(crossReferenceAnalysisStep,
                "analyze-site",
                "generate-platform-summaries")

            // Quality scoring and insights
            .addStep(generateScoringInsightsStep,
                "cross-reference-analysis")

            // ====================================================================
            // PHASE 4: FINAL (1 step)
            // ====================================================================
            // Generate comprehensive final profile
            .addStep(generateFinalProfileStep,
                "cross-reference-analysis",
                "generate-scoring")

            .build();
    }

    /**
     * Get workflow metadata for monitoring/logging
     */
    public WorkflowMetadata getMetadata() {
        return new WorkflowMetadata(
            "UltraDeepAnalysisWorkflow",
            24, // implemented steps
            0,  // pending steps
            4,  // phases
            Duration.ofHours(3)
        );
    }

    /**
     * Simple record for workflow metadata
     */
    public record WorkflowMetadata(
        String name,
        int implementedSteps,
        int pendingSteps,
        int phases,
        Duration estimatedDuration
    ) {
        public int totalSteps() {
            return implementedSteps + pendingSteps;
        }

        public double completionPercentage() {
            return (double) implementedSteps / totalSteps() * 100.0;
        }
    }
}
