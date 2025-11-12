package fr.postiqa.core.infrastructure.workflow.ultradeep.step.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import fr.postiqa.database.entity.ScoringInsightsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 3C: Generate scoring insights with 10 quantitative scores and actionable recommendations.
 * <p>
 * Uses cross-reference analysis to calculate:
 * - 10 core scores (1-10 scale)
 * - Benchmarking and maturity assessment
 * - Prioritized actionable insights
 * - Content opportunities per platform
 */
@Slf4j
@Component
public class GenerateScoringInsightsStep implements WorkflowStep<Void, Map<String, Object>> {

    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    public GenerateScoringInsightsStep(
        OpenAIClient openAIClient,
        JpaAnalysisResultsAdapter analysisAdapter,
        ObjectMapper objectMapper
    ) {
        this.openAIClient = openAIClient;
        this.analysisAdapter = analysisAdapter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "generate-scoring-insights";
    }

    @Override
    public String getStepName() {
        return "Generate Scoring and Insights";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);

        log.info("Generating scoring insights and actionable recommendations");

        // Get cross-reference analysis and platform summaries
        Map<String, Object> crossRefAnalysis = context.getRequired("cross_reference_result", Map.class);
        Map<String, Map<String, Object>> platformSummaries = context.getRequired("platform_summaries", Map.class);

        // Build comprehensive scoring prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            Based on the comprehensive cross-platform profile and platform summaries, generate quantitative scores and actionable insights.

            === CROSS-REFERENCE ANALYSIS ===
            """);
        prompt.append(objectMapper.writeValueAsString(crossRefAnalysis)).append("\n\n");

        prompt.append("=== PLATFORM SUMMARIES ===\n");
        prompt.append(objectMapper.writeValueAsString(platformSummaries)).append("\n\n");

        prompt.append("""

            Generate a comprehensive scoring and insights analysis:

            {
              "scores": {
                "overall_content_quality_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "brand_consistency_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "engagement_effectiveness_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "thought_leadership_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "authenticity_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "visual_branding_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "posting_consistency_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "audience_building_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "conversion_optimization_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                },
                "innovation_score": {
                  "score": <1-10>,
                  "rationale": "<detailed explanation>",
                  "evidence": ["<evidence1>", "<evidence2>"]
                }
              },
              "overall_score": {
                "composite_score": <weighted average of all scores, 1-10>,
                "score_distribution": "<assessment of strengths and weaknesses>",
                "percentile_estimate": "<estimated percentile vs. typical creator>"
              },
              "benchmarking": {
                "content_maturity_level": "<beginner/intermediate/advanced/expert>",
                "maturity_assessment": "<detailed assessment>",
                "comparison_to_benchmarks": {
                  "content_quality": "<below/at/above benchmark>",
                  "engagement": "<below/at/above benchmark>",
                  "consistency": "<below/at/above benchmark>",
                  "brand_development": "<below/at/above benchmark>"
                },
                "percentile_by_category": {
                  "content_creators": "<percentile>",
                  "business_professionals": "<percentile>",
                  "industry_specific": "<percentile>"
                },
                "growth_trajectory_assessment": "<accelerating/steady/plateauing/declining>"
              },
              "actionable_insights": [
                {
                  "insight": "<specific, actionable insight>",
                  "category": "<content/engagement/branding/technical/strategy>",
                  "impact": "<low/medium/high>",
                  "effort": "<low/medium/high>",
                  "priority_score": <1-10>,
                  "implementation_steps": ["<step1>", "<step2>", "<step3>"],
                  "expected_outcome": "<specific outcome>",
                  "timeframe": "<days/weeks/months>"
                },
                {
                  "insight": "<specific, actionable insight>",
                  "category": "<content/engagement/branding/technical/strategy>",
                  "impact": "<low/medium/high>",
                  "effort": "<low/medium/high>",
                  "priority_score": <1-10>,
                  "implementation_steps": ["<step1>", "<step2>", "<step3>"],
                  "expected_outcome": "<specific outcome>",
                  "timeframe": "<days/weeks/months>"
                }
                // Continue for 10-15 insights, prioritized by priority_score
              ],
              "content_opportunities": {
                "high_potential_formats": [
                  {
                    "format": "<format name>",
                    "platforms": ["<platform1>", "<platform2>"],
                    "rationale": "<why this would work>",
                    "example_topics": ["<topic1>", "<topic2>"]
                  }
                ],
                "underutilized_platforms": [
                  {
                    "platform": "<platform>",
                    "opportunity": "<specific opportunity>",
                    "approach": "<recommended approach>"
                  }
                ],
                "content_gaps_to_fill": [
                  {
                    "gap": "<specific gap>",
                    "audience_need": "<what audience wants>",
                    "recommendation": "<how to fill gap>"
                  }
                ],
                "quick_wins": [
                  {
                    "opportunity": "<quick win>",
                    "implementation": "<how to do it>",
                    "expected_impact": "<impact>"
                  }
                ]
              },
              "growth_roadmap": {
                "30_day_priorities": ["<priority1>", "<priority2>", "<priority3>"],
                "90_day_priorities": ["<priority1>", "<priority2>", "<priority3>"],
                "long_term_vision": "<where to focus for sustained growth>"
              }
            }

            CRITICAL SCORING GUIDELINES:
            - Score 1-3: Significant issues, needs major improvement
            - Score 4-6: Acceptable but room for growth
            - Score 7-8: Good, competitive level
            - Score 9-10: Exceptional, industry-leading

            Be honest and evidence-based. Provide specific, actionable recommendations.

            Return ONLY the JSON object, no additional text.
            """);

        String systemInstruction = """
            You are an expert content strategist and digital marketing analyst specializing in:
            - Content performance analysis
            - Personal branding assessment
            - Social media strategy
            - Audience engagement optimization

            Your task is to provide honest, evidence-based scores with actionable insights.
            Be specific and prioritize recommendations by impact and effort.
            Think like a consultant who gets paid for results.
            """;

        log.info("Calling OpenAI for scoring insights generation...");
        String response = openAIClient.complete(systemInstruction, prompt.toString());

        // Parse JSON response
        String jsonString = response.trim();
        if (jsonString.startsWith("```json")) {
            jsonString = jsonString.substring(7);
        } else if (jsonString.startsWith("```")) {
            jsonString = jsonString.substring(3);
        }
        if (jsonString.endsWith("```")) {
            jsonString = jsonString.substring(0, jsonString.length() - 3);
        }

        Map<String, Object> scoringData = objectMapper.readValue(jsonString.trim(), Map.class);

        // Extract individual scores for entity fields
        Map<String, Object> scores = (Map<String, Object>) scoringData.get("scores");

        // Build all_scores map
        Map<String, Object> allScoresMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : scores.entrySet()) {
            Map<String, Object> scoreDetail = (Map<String, Object>) entry.getValue();
            allScoresMap.put(entry.getKey(), scoreDetail.get("score"));
        }

        // Save to database
        ScoringInsightsEntity entity = ScoringInsightsEntity.builder()
            .overallContentQualityScore(extractScore(scores, "overall_content_quality_score"))
            .brandConsistencyScore(extractScore(scores, "brand_consistency_score"))
            .engagementEffectivenessScore(extractScore(scores, "engagement_effectiveness_score"))
            .thoughtLeadershipScore(extractScore(scores, "thought_leadership_score"))
            .authenticityScore(extractScore(scores, "authenticity_score"))
            .visualBrandingScore(extractScore(scores, "visual_branding_score"))
            .postingConsistencyScore(extractScore(scores, "posting_consistency_score"))
            .audienceBuildingScore(extractScore(scores, "audience_building_score"))
            .conversionOptimizationScore(extractScore(scores, "conversion_optimization_score"))
            .innovationScore(extractScore(scores, "innovation_score"))
            .allScores(allScoresMap)
            .benchmarking((Map<String, Object>) scoringData.get("benchmarking"))
            .actionableInsights((Map<String, Object>) scoringData.get("actionable_insights"))
            .contentOpportunities((Map<String, Object>) scoringData.get("content_opportunities"))
            .build();

        UUID scoringId = analysisAdapter.saveScoringInsights(userProfileAnalysisId, entity);

        log.info("Saved scoring insights to database with ID: {}", scoringId);

        // Calculate average score for logging
        double avgScore = allScoresMap.values().stream()
            .mapToInt(v -> (Integer) v)
            .average()
            .orElse(0.0);

        log.info("Overall average score: {}/10", String.format("%.1f", avgScore));

        // Return complete result with metadata
        Map<String, Object> result = new HashMap<>(scoringData);
        result.put("scoring_id", scoringId.toString());
        result.put("average_score", avgScore);

        return result;
    }

    private Integer extractScore(Map<String, Object> scores, String scoreKey) {
        Map<String, Object> scoreDetail = (Map<String, Object>) scores.get(scoreKey);
        if (scoreDetail == null) {
            log.warn("Score not found: {}", scoreKey);
            return 5; // Default to middle score
        }
        Object scoreValue = scoreDetail.get("score");
        if (scoreValue instanceof Integer) {
            return (Integer) scoreValue;
        } else if (scoreValue instanceof Double) {
            return ((Double) scoreValue).intValue();
        }
        return 5; // Default fallback
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("scoring_result");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(10); // Scoring requires detailed analysis
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(10));
    }
}
