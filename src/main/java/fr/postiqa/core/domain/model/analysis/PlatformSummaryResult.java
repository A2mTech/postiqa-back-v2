package fr.postiqa.core.domain.model.analysis;

import fr.postiqa.core.domain.enums.SocialPlatform;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Aggregated summary for a single social media platform.
 * Synthesizes profile and posts analysis into overall patterns.
 * Maps to Phase 3A in the ultra-deep analysis workflow.
 */
public record PlatformSummaryResult(
    SocialPlatform platform,
    int totalPostsAnalyzed,
    Map<String, Object> profileQuality,
    Map<String, Object> contentPatterns,
    Map<String, Object> writingStyleProfile,
    Map<String, Object> brandAlignment,
    Map<String, Object> audienceEngagement,
    List<String> recommendations
) {
    public PlatformSummaryResult {
        profileQuality = profileQuality != null ? Map.copyOf(profileQuality) : Collections.emptyMap();
        contentPatterns = contentPatterns != null ? Map.copyOf(contentPatterns) : Collections.emptyMap();
        writingStyleProfile = writingStyleProfile != null ? Map.copyOf(writingStyleProfile) : Collections.emptyMap();
        brandAlignment = brandAlignment != null ? Map.copyOf(brandAlignment) : Collections.emptyMap();
        audienceEngagement = audienceEngagement != null ? Map.copyOf(audienceEngagement) : Collections.emptyMap();
        recommendations = recommendations != null ? List.copyOf(recommendations) : Collections.emptyList();
    }
}

/**
 * Cross-platform analysis result combining all platform summaries.
 * Maps to Phase 3B in the ultra-deep analysis workflow.
 */
record CrossReferenceResult(
    Map<String, Object> identity,
    Map<String, Object> businessPresence,
    Map<String, Object> personalBrand,
    Map<String, Object> contentDNA,
    Map<String, Object> crossPlatformInsights,
    List<String> strengths,
    List<String> weaknesses,
    List<String> opportunities,
    Map<String, Object> strategicRecommendations
) {
    public CrossReferenceResult {
        identity = identity != null ? Map.copyOf(identity) : Collections.emptyMap();
        businessPresence = businessPresence != null ? Map.copyOf(businessPresence) : Collections.emptyMap();
        personalBrand = personalBrand != null ? Map.copyOf(personalBrand) : Collections.emptyMap();
        contentDNA = contentDNA != null ? Map.copyOf(contentDNA) : Collections.emptyMap();
        crossPlatformInsights = crossPlatformInsights != null ? Map.copyOf(crossPlatformInsights) : Collections.emptyMap();
        strengths = strengths != null ? List.copyOf(strengths) : Collections.emptyList();
        weaknesses = weaknesses != null ? List.copyOf(weaknesses) : Collections.emptyList();
        opportunities = opportunities != null ? List.copyOf(opportunities) : Collections.emptyList();
        strategicRecommendations = strategicRecommendations != null ? Map.copyOf(strategicRecommendations) : Collections.emptyMap();
    }
}

/**
 * Scoring and insights result with actionable recommendations.
 * Maps to Phase 3C in the ultra-deep analysis workflow.
 */
record ScoringResult(
    Map<String, Integer> scores,  // All scores (1-10) by name
    String contentMaturityLevel,  // beginner, intermediate, advanced, expert
    int percentile,  // 0-100
    List<ActionableInsight> insights,
    List<ContentOpportunity> opportunities
) {
    public ScoringResult {
        scores = scores != null ? Map.copyOf(scores) : Collections.emptyMap();
        insights = insights != null ? List.copyOf(insights) : Collections.emptyList();
        opportunities = opportunities != null ? List.copyOf(opportunities) : Collections.emptyList();
    }

    public int getOverallScore() {
        return scores.getOrDefault("overall_content_quality", 0);
    }
}

/**
 * Single actionable insight with priority and impact/effort assessment
 */
record ActionableInsight(
    String insight,
    String priority,  // high, medium, low
    String category,  // content, branding, engagement, technical
    String expectedImpact,
    String effortRequired  // low, medium, high
) {}

/**
 * Content opportunity for a specific platform
 */
record ContentOpportunity(
    String opportunity,
    String platform,
    String why,
    String how
) {}

/**
 * Final unified profile for content generation.
 * Maps to Phase 4 in the ultra-deep analysis workflow.
 */
record FinalProfile(
    String userId,
    Map<String, Object> userIdentity,
    Map<String, Object> businessContext,
    Map<String, Object> brandProfile,
    Map<String, Object> writingStyleDNA,
    Map<String, Object> platformStrategies,
    Map<String, Object> contentGenerationGuidelines,
    Map<String, Object> visualBrandGuidelines,
    Map<String, Object> engagementPatterns
) {
    public FinalProfile {
        userIdentity = userIdentity != null ? Map.copyOf(userIdentity) : Collections.emptyMap();
        businessContext = businessContext != null ? Map.copyOf(businessContext) : Collections.emptyMap();
        brandProfile = brandProfile != null ? Map.copyOf(brandProfile) : Collections.emptyMap();
        writingStyleDNA = writingStyleDNA != null ? Map.copyOf(writingStyleDNA) : Collections.emptyMap();
        platformStrategies = platformStrategies != null ? Map.copyOf(platformStrategies) : Collections.emptyMap();
        contentGenerationGuidelines = contentGenerationGuidelines != null ? Map.copyOf(contentGenerationGuidelines) : Collections.emptyMap();
        visualBrandGuidelines = visualBrandGuidelines != null ? Map.copyOf(visualBrandGuidelines) : Collections.emptyMap();
        engagementPatterns = engagementPatterns != null ? Map.copyOf(engagementPatterns) : Collections.emptyMap();
    }
}
