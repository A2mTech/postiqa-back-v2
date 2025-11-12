package fr.postiqa.core.infrastructure.workflow.ultradeep.step.finalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Step 4: Generate the ultimate final profile for content generation.
 * <p>
 * Synthesizes:
 * - Cross-reference analysis
 * - Scoring insights
 * - All platform summaries
 * <p>
 * Creates the definitive profile optimized for AI content generation that matches the user's style.
 * Updates the database with final profile and marks analysis as COMPLETED.
 */
@Slf4j
@Component
public class GenerateFinalProfileStep implements WorkflowStep<Void, Map<String, Object>> {

    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    public GenerateFinalProfileStep(
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
        return "generate-final-profile";
    }

    @Override
    public String getStepName() {
        return "Generate Final Profile";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);

        log.info("Generating final profile - the ultimate synthesis for content generation");

        // Gather all analysis results
        Map<String, Object> crossRefAnalysis = context.getRequired("cross_reference_result", Map.class);
        Map<String, Object> scoringInsights = context.getRequired("scoring_result", Map.class);
        Map<String, Map<String, Object>> platformSummaries = context.getRequired("platform_summaries", Map.class);

        // Build comprehensive final profile prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            You are creating the ULTIMATE FINAL PROFILE for AI content generation.

            This profile will be used by AI systems to generate content that perfectly matches the user's authentic voice, style, and brand.

            CRITICAL MISSION: Create the most comprehensive, actionable, and precise profile possible.
            Every detail matters. Every pattern must be captured. This is the foundation for all future content.

            === CROSS-REFERENCE ANALYSIS ===
            """);
        prompt.append(objectMapper.writeValueAsString(crossRefAnalysis)).append("\n\n");

        prompt.append("=== SCORING INSIGHTS ===\n");
        prompt.append(objectMapper.writeValueAsString(scoringInsights)).append("\n\n");

        prompt.append("=== PLATFORM SUMMARIES ===\n");
        prompt.append(objectMapper.writeValueAsString(platformSummaries)).append("\n\n");

        prompt.append("""

            Generate the FINAL COMPREHENSIVE PROFILE optimized for content generation:

            {
              "profile_metadata": {
                "profile_version": "1.0",
                "generation_timestamp": "<ISO timestamp>",
                "confidence_level": "<high/medium/low based on data availability>",
                "data_completeness_score": <1-10>,
                "platforms_analyzed": <number>,
                "total_posts_analyzed": <number>
              },
              "identity_and_brand": {
                "personal_identity": {
                  "full_name": "<name>",
                  "professional_title": "<title>",
                  "bio": "<comprehensive bio 3-5 sentences>",
                  "expertise_areas": ["<area1>", "<area2>", "<area3>"],
                  "credentials": ["<credential1>", "<credential2>"],
                  "unique_positioning": "<what makes them uniquely valuable>"
                },
                "business_identity": {
                  "business_name": "<name>",
                  "business_type": "<type>",
                  "value_proposition": "<clear value prop>",
                  "target_audience": {
                    "primary": "<primary audience>",
                    "secondary": ["<secondary1>", "<secondary2>"],
                    "psychographics": ["<trait1>", "<trait2>"]
                  },
                  "services_products": ["<offering1>", "<offering2>"]
                },
                "brand_essence": {
                  "positioning_statement": "<how they want to be perceived>",
                  "brand_archetype": "<archetype>",
                  "brand_pillars": ["<pillar1>", "<pillar2>", "<pillar3>"],
                  "brand_promise": "<what audience can expect>",
                  "differentiation": "<key differentiators>"
                }
              },
              "content_dna": {
                "voice_profile": {
                  "primary_tone": "<tone>",
                  "tone_variations": {
                    "professional_contexts": "<tone>",
                    "casual_contexts": "<tone>",
                    "inspirational_contexts": "<tone>"
                  },
                  "voice_characteristics": ["<characteristic1>", "<characteristic2>"],
                  "personality_in_writing": ["<trait1>", "<trait2>", "<trait3>"]
                },
                "writing_style": {
                  "sentence_structure": {
                    "typical_length": "<short/medium/long/varied>",
                    "complexity": "<simple/moderate/complex>",
                    "patterns": ["<pattern1>", "<pattern2>"]
                  },
                  "vocabulary": {
                    "level": "<accessible/intermediate/advanced/sophisticated>",
                    "jargon_usage": "<minimal/moderate/frequent>",
                    "signature_phrases": ["<phrase1>", "<phrase2>"]
                  },
                  "formatting_preferences": {
                    "paragraph_style": "<short/medium/long>",
                    "use_of_lists": "<frequent/occasional/rare>",
                    "emoji_usage": "<none/minimal/moderate/heavy>",
                    "emoji_patterns": ["<pattern1>", "<pattern2>"],
                    "line_break_style": "<description>",
                    "punctuation_style": ["<style1>", "<style2>"]
                  }
                },
                "storytelling_approach": {
                  "narrative_structures": ["<structure1>", "<structure2>"],
                  "hook_techniques": ["<technique1>", "<technique2>", "<technique3>"],
                  "story_types": ["<type1>", "<type2>"],
                  "emotional_arc_preferences": "<description>",
                  "pacing_style": "<fast/moderate/slow/varied>"
                },
                "content_patterns": {
                  "core_themes": ["<theme1>", "<theme2>", "<theme3>"],
                  "content_pillars": ["<pillar1>", "<pillar2>", "<pillar3>"],
                  "topic_preferences": ["<topic1>", "<topic2>"],
                  "avoided_topics": ["<topic1>", "<topic2>"],
                  "content_mix": {
                    "educational": "<percentage or description>",
                    "inspirational": "<percentage or description>",
                    "promotional": "<percentage or description>",
                    "personal": "<percentage or description>",
                    "entertainment": "<percentage or description>"
                  }
                }
              },
              "content_structure_templates": {
                "text_posts": {
                  "structure": "<typical structure>",
                  "hook_patterns": ["<pattern1>", "<pattern2>"],
                  "body_patterns": ["<pattern1>", "<pattern2>"],
                  "cta_patterns": ["<pattern1>", "<pattern2>"],
                  "length_preferences": {
                    "short": "<character range>",
                    "medium": "<character range>",
                    "long": "<character range>"
                  }
                },
                "visual_content": {
                  "image_style": "<description>",
                  "carousel_approach": "<description>",
                  "design_preferences": ["<preference1>", "<preference2>"],
                  "text_overlay_style": "<description>"
                },
                "video_content": {
                  "script_structure": "<structure>",
                  "hook_style": "<description>",
                  "pacing": "<description>",
                  "production_style": "<description>"
                },
                "threads": {
                  "structure": "<typical thread structure>",
                  "tweet_count_preference": "<range>",
                  "transition_style": "<description>",
                  "conclusion_style": "<description>"
                }
              },
              "platform_strategies": {
                "per_platform_adaptation": {
                  "linkedin": {
                    "content_focus": ["<focus1>", "<focus2>"],
                    "tone_adaptation": "<description>",
                    "optimal_formats": ["<format1>", "<format2>"],
                    "posting_patterns": "<description>"
                  },
                  "twitter": {
                    "content_focus": ["<focus1>", "<focus2>"],
                    "tone_adaptation": "<description>",
                    "optimal_formats": ["<format1>", "<format2>"],
                    "posting_patterns": "<description>"
                  },
                  "instagram": {
                    "content_focus": ["<focus1>", "<focus2>"],
                    "tone_adaptation": "<description>",
                    "optimal_formats": ["<format1>", "<format2>"],
                    "posting_patterns": "<description>"
                  }
                  // Add other platforms as available
                },
                "cross_platform_consistency": {
                  "core_message": "<consistent message across all>",
                  "adapted_elements": ["<element1>", "<element2>"],
                  "platform_specific_nuances": "<description>"
                }
              },
              "engagement_style": {
                "call_to_action_patterns": ["<pattern1>", "<pattern2>", "<pattern3>"],
                "question_usage": "<frequent/moderate/rare>",
                "question_types": ["<type1>", "<type2>"],
                "community_interaction_style": "<description>",
                "response_patterns": ["<pattern1>", "<pattern2>"]
              },
              "content_quality_standards": {
                "what_makes_their_best_content": ["<factor1>", "<factor2>", "<factor3>"],
                "content_principles": ["<principle1>", "<principle2>"],
                "quality_indicators": ["<indicator1>", "<indicator2>"],
                "avoid_patterns": ["<pattern1>", "<pattern2>"]
              },
              "ai_generation_guidelines": {
                "must_include": [
                  "<guideline1: e.g., always use personal anecdotes>",
                  "<guideline2: e.g., end with actionable takeaway>",
                  "<guideline3>"
                ],
                "must_avoid": [
                  "<guideline1: e.g., never use corporate jargon>",
                  "<guideline2: e.g., avoid clickbait tactics>",
                  "<guideline3>"
                ],
                "tone_calibration": {
                  "authenticity_level": "<scale description>",
                  "professionalism_level": "<scale description>",
                  "casualness_level": "<scale description>",
                  "inspiration_level": "<scale description>"
                },
                "voice_matching_priorities": [
                  "<priority1: e.g., maintain conversational tone>",
                  "<priority2: e.g., use signature formatting>",
                  "<priority3>"
                ],
                "content_generation_rules": [
                  "<rule1>",
                  "<rule2>",
                  "<rule3>"
                ]
              },
              "examples_and_templates": {
                "best_performing_patterns": [
                  {
                    "pattern_name": "<name>",
                    "pattern_type": "<type>",
                    "structure": "<structure>",
                    "why_it_works": "<explanation>",
                    "example": "<actual example from their content>"
                  }
                ],
                "signature_openings": [
                  "<example1>",
                  "<example2>",
                  "<example3>"
                ],
                "signature_closings": [
                  "<example1>",
                  "<example2>",
                  "<example3>"
                ],
                "typical_transitions": [
                  "<transition1>",
                  "<transition2>",
                  "<transition3>"
                ]
              },
              "performance_insights": {
                "what_resonates_with_audience": ["<insight1>", "<insight2>"],
                "content_strengths_to_leverage": ["<strength1>", "<strength2>"],
                "optimization_opportunities": ["<opportunity1>", "<opportunity2>"],
                "recommended_experiments": ["<experiment1>", "<experiment2>"]
              },
              "strategic_context": {
                "current_positioning": "<where they are now>",
                "aspirational_positioning": "<where they want to be>",
                "content_evolution_direction": "<how content should evolve>",
                "brand_development_priorities": ["<priority1>", "<priority2>"]
              }
            }

            CRITICAL INSTRUCTIONS FOR AI CONTENT GENERATION:
            - This profile must enable AI to generate content indistinguishable from the user's authentic voice
            - Capture EVERY nuance, pattern, and subtlety
            - Be extremely specific with examples
            - Include direct quotes and patterns from actual content
            - Prioritize accuracy and authenticity over everything else

            Return ONLY the JSON object, no additional text.
            """);

        String systemInstruction = """
            You are the MASTER SYNTHESIZER creating the ultimate content generation profile.

            Your output will be used by AI systems to generate content that must be:
            1. Authentic - perfectly matching the user's voice
            2. Actionable - immediately usable for content generation
            3. Comprehensive - capturing every relevant detail
            4. Precise - no vague descriptions, only specific patterns

            This is the culmination of the entire ultra-deep analysis. Make it perfect.
            Think like you're creating a blueprint that another AI must follow exactly.
            Be extremely detailed and specific.
            """;

        log.info("Calling OpenAI for final profile generation (this is the big one)...");
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

        Map<String, Object> finalProfile = objectMapper.readValue(jsonString.trim(), Map.class);

        // Update database with final profile
        analysisAdapter.updateFinalProfile(userProfileAnalysisId, finalProfile);
        log.info("Updated database with final profile");

        // Mark analysis as COMPLETED
        analysisAdapter.updateAnalysisStatus(userProfileAnalysisId, "COMPLETED");
        log.info("Analysis status updated to COMPLETED");

        log.info("ULTRA-DEEP ANALYSIS WORKFLOW COMPLETE!");
        log.info("Final profile generated with {} top-level sections", finalProfile.size());

        // Return final profile with completion metadata
        Map<String, Object> result = new HashMap<>(finalProfile);
        result.put("analysis_id", userProfileAnalysisId.toString());
        result.put("status", "COMPLETED");
        result.put("completion_timestamp", java.time.Instant.now().toString());

        return result;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("final_profile");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(10); // Final synthesis
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(10));
    }
}
