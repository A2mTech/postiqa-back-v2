package fr.postiqa.core.infrastructure.workflow.ultradeep.step.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import fr.postiqa.database.entity.CrossReferenceAnalysisEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 3B: Cross-reference all platform summaries + site analysis into unified global profile.
 * <p>
 * Aggregates:
 * - All platform summaries
 * - Site analysis
 * - Cross-platform patterns
 * <p>
 * Generates comprehensive cross-platform insights covering identity, business presence,
 * personal brand, content DNA, and strategic recommendations.
 */
@Slf4j
@Component
public class CrossReferenceAnalysisStep implements WorkflowStep<Void, Map<String, Object>> {

    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    public CrossReferenceAnalysisStep(
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
        return "cross-reference-analysis";
    }

    @Override
    public String getStepName() {
        return "Cross-Reference Analysis";
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getFromContext(WorkflowContext context, String key, Class<?> type) {
        return (Optional<T>) (Optional<?>) context.get(key, type);
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);

        log.info("Generating cross-reference analysis across all platforms and site data");

        // Gather all aggregated data
        Map<String, Map<String, Object>> platformSummaries = context.getRequired("platform_summaries", Map.class);
        Optional<Map<String, Object>> siteAnalysisOpt = getFromContext(context, "site_analysis", Map.class);

        // Build comprehensive cross-platform prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            You are synthesizing ALL available data from multiple platforms and website into a unified, cross-platform profile.

            This is the MOST IMPORTANT analysis - it creates the foundation for all future content generation.

            """);

        // Add site analysis if available
        if (siteAnalysisOpt.isPresent()) {
            prompt.append("=== WEBSITE ANALYSIS ===\n");
            prompt.append(objectMapper.writeValueAsString(siteAnalysisOpt.get())).append("\n\n");
        }

        // Add all platform summaries
        prompt.append("=== PLATFORM SUMMARIES ===\n");
        prompt.append(String.format("Platforms analyzed: %d\n\n", platformSummaries.size()));

        for (Map.Entry<String, Map<String, Object>> entry : platformSummaries.entrySet()) {
            prompt.append(String.format("--- %s SUMMARY ---\n", entry.getKey()));
            prompt.append(objectMapper.writeValueAsString(entry.getValue())).append("\n\n");
        }

        prompt.append("""

            Based on ALL the data above, generate a comprehensive cross-platform profile:

            {
              "identity": {
                "full_name": "<name>",
                "professional_title": "<title>",
                "expertise_areas": ["<area1>", "<area2>", "<area3>"],
                "professional_summary": "<2-3 sentence summary>",
                "location": "<location if available>",
                "languages": ["<language1>", "<language2>"]
              },
              "business_presence": {
                "business_type": "<solopreneur/agency/consultant/corporate/creator/etc>",
                "business_name": "<name if different from personal>",
                "value_proposition": "<clear value prop>",
                "target_audience": "<who they serve>",
                "service_offering": ["<service1>", "<service2>"],
                "business_maturity": "<startup/growing/established/mature>",
                "monetization_model": "<how they monetize>"
              },
              "personal_brand": {
                "positioning_statement": "<how they position themselves>",
                "unique_value": "<what makes them unique>",
                "brand_archetype": "<archetype: expert/educator/innovator/etc>",
                "brand_consistency_score": <1-10>,
                "brand_maturity_level": "<emerging/developing/established/iconic>",
                "brand_pillars": ["<pillar1>", "<pillar2>", "<pillar3>"]
              },
              "content_dna": {
                "unified_voice": {
                  "primary_tone": "<tone>",
                  "secondary_tones": ["<tone1>", "<tone2>"],
                  "voice_characteristics": ["<characteristic1>", "<characteristic2>"]
                },
                "core_themes": ["<theme1>", "<theme2>", "<theme3>"],
                "content_pillars": ["<pillar1>", "<pillar2>", "<pillar3>"],
                "signature_formats": ["<format1>", "<format2>"],
                "writing_patterns": {
                  "sentence_structure": "<description>",
                  "vocabulary_level": "<simple/moderate/advanced/sophisticated>",
                  "storytelling_approach": "<approach>",
                  "hook_patterns": ["<pattern1>", "<pattern2>"]
                },
                "authenticity_score": <1-10>,
                "consistency_across_platforms": <1-10>
              },
              "cross_platform_insights": {
                "platform_strategy": {
                  "primary_platform": "<platform>",
                  "secondary_platforms": ["<platform1>", "<platform2>"],
                  "platform_specific_adaptations": "<how content adapts per platform>"
                },
                "content_repurposing_patterns": ["<pattern1>", "<pattern2>"],
                "cross_platform_consistency": "<assessment>",
                "unified_message": "<core message across all platforms>",
                "platform_synergies": ["<synergy1>", "<synergy2>"]
              },
              "audience_relationship": {
                "engagement_style": "<how they engage with audience>",
                "community_building_approach": "<approach>",
                "audience_interaction_frequency": "<low/medium/high>",
                "relationship_depth": "<transactional/growing/deep/loyal>",
                "audience_perception": "<how audience likely perceives them>"
              },
              "growth_trajectory": {
                "content_evolution": "<how content has evolved>",
                "consistency_over_time": "<assessment>",
                "quality_progression": "<improving/stable/declining>",
                "experimentation_level": "<low/medium/high>",
                "growth_stage": "<early/scaling/mature>"
              },
              "strengths": {
                "content_strengths": ["<strength1>", "<strength2>", "<strength3>"],
                "platform_strengths": ["<strength1>", "<strength2>"],
                "brand_strengths": ["<strength1>", "<strength2>"],
                "overall_competitive_advantages": ["<advantage1>", "<advantage2>"]
              },
              "weaknesses": {
                "content_gaps": ["<gap1>", "<gap2>"],
                "platform_weaknesses": ["<weakness1>", "<weakness2>"],
                "brand_inconsistencies": ["<inconsistency1>", "<inconsistency2>"],
                "areas_for_improvement": ["<area1>", "<area2>"]
              },
              "opportunities": {
                "untapped_platforms": ["<platform1>", "<platform2>"],
                "content_format_opportunities": ["<format1>", "<format2>"],
                "audience_expansion_opportunities": ["<opportunity1>", "<opportunity2>"],
                "collaboration_potential": ["<potential1>", "<potential2>"],
                "monetization_opportunities": ["<opportunity1>", "<opportunity2>"]
              },
              "strategic_recommendations": {
                "immediate_priorities": [
                  {
                    "priority": "<priority 1>",
                    "rationale": "<why>",
                    "expected_impact": "<impact>",
                    "effort_required": "<low/medium/high>"
                  },
                  {
                    "priority": "<priority 2>",
                    "rationale": "<why>",
                    "expected_impact": "<impact>",
                    "effort_required": "<low/medium/high>"
                  }
                ],
                "content_strategy_evolution": ["<recommendation1>", "<recommendation2>"],
                "platform_strategy_evolution": ["<recommendation1>", "<recommendation2>"],
                "brand_development": ["<recommendation1>", "<recommendation2>"],
                "growth_tactics": ["<tactic1>", "<tactic2>"]
              },
              "meta_insights": {
                "overall_digital_presence_maturity": "<beginner/intermediate/advanced/expert>",
                "content_creator_archetype": "<archetype>",
                "future_trajectory_projection": "<projection>",
                "unique_differentiators": ["<differentiator1>", "<differentiator2>"]
              }
            }

            CRITICAL: Be extremely detailed and specific. This profile will be used to generate content in the user's style.
            Extract every possible pattern, nuance, and insight. Be thorough.

            Return ONLY the JSON object, no additional text.
            """);

        String systemInstruction = """
            You are an expert digital strategist and brand analyst with deep expertise in:
            - Personal branding and positioning
            - Content strategy across platforms
            - Writing style analysis
            - Audience psychology
            - Business development

            Your task is to synthesize ALL available data into the most comprehensive, actionable profile possible.
            This profile will be the foundation for AI-generated content that matches the user's authentic voice.

            Be specific, detailed, and insightful. Find patterns others would miss.
            """;

        log.info("Calling OpenAI for cross-reference analysis (this may take several minutes)...");
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

        Map<String, Object> crossRefData = objectMapper.readValue(jsonString.trim(), Map.class);

        // Save to database
        CrossReferenceAnalysisEntity entity = CrossReferenceAnalysisEntity.builder()
            .identity((Map<String, Object>) crossRefData.get("identity"))
            .businessPresence((Map<String, Object>) crossRefData.get("business_presence"))
            .personalBrand((Map<String, Object>) crossRefData.get("personal_brand"))
            .contentDNA((Map<String, Object>) crossRefData.get("content_dna"))
            .crossPlatformInsights((Map<String, Object>) crossRefData.get("cross_platform_insights"))
            .audienceRelationship((Map<String, Object>) crossRefData.get("audience_relationship"))
            .growthTrajectory((Map<String, Object>) crossRefData.get("growth_trajectory"))
            .strengths((Map<String, Object>) crossRefData.get("strengths"))
            .weaknesses((Map<String, Object>) crossRefData.get("weaknesses"))
            .opportunities((Map<String, Object>) crossRefData.get("opportunities"))
            .strategicRecommendations((Map<String, Object>) crossRefData.get("strategic_recommendations"))
            .build();

        UUID crossRefId = analysisAdapter.saveCrossReferenceAnalysis(userProfileAnalysisId, entity);

        log.info("Saved cross-reference analysis to database with ID: {}", crossRefId);

        // Return complete result with metadata
        Map<String, Object> result = new HashMap<>(crossRefData);
        result.put("cross_reference_id", crossRefId.toString());
        result.put("platforms_analyzed", platformSummaries.size());
        result.put("has_site_analysis", siteAnalysisOpt.isPresent());

        return result;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("cross_reference_result");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(15); // Large synthesis task
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(10));
    }
}
