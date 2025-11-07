package fr.postiqa.core.infrastructure.client.analysis.strategy;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.AnalysisResult;

import java.util.List;

/**
 * Strategy interface for different types of analysis
 * Each implementation handles a specific analysis type (writing style, content themes, image, multimodal)
 *
 * @param <T> Type of analysis result
 */
public interface AnalysisStrategy<T> {

    /**
     * Get the analysis type this strategy handles
     *
     * @return AnalysisType enum value
     */
    AnalysisType getAnalysisType();

    /**
     * Analyze a single post
     *
     * @param post Post to analyze
     * @param provider AI provider to use
     * @return Analysis result wrapped in AnalysisResult
     */
    AnalysisResult<T> analyzeSinglePost(SocialPost post, AIProvider provider);

    /**
     * Analyze multiple posts in batch
     *
     * @param posts Posts to analyze together
     * @param provider AI provider to use
     * @return Analysis result wrapped in AnalysisResult
     */
    AnalysisResult<T> analyzeBatchPosts(List<SocialPost> posts, AIProvider provider);

    /**
     * Analyze full profile across all posts
     *
     * @param posts All posts from the profile
     * @param provider AI provider to use
     * @return Analysis result wrapped in AnalysisResult
     */
    AnalysisResult<T> analyzeFullProfile(List<SocialPost> posts, AIProvider provider);

    /**
     * Generic analyze method that dispatches based on granularity
     *
     * @param posts Posts to analyze (single or multiple)
     * @param granularity Granularity level
     * @param provider AI provider to use
     * @return Analysis result
     */
    default AnalysisResult<T> analyze(List<SocialPost> posts, AnalysisGranularity granularity, AIProvider provider) {
        return switch (granularity) {
            case SINGLE_POST -> {
                if (posts == null || posts.isEmpty()) {
                    throw new IllegalArgumentException("No posts provided for SINGLE_POST analysis");
                }
                yield analyzeSinglePost(posts.get(0), provider);
            }
            case BATCH_POSTS -> analyzeBatchPosts(posts, provider);
            case FULL_PROFILE -> analyzeFullProfile(posts, provider);
        };
    }

    /**
     * Check if this strategy supports the given provider
     *
     * @param provider AI provider
     * @return true if supported
     */
    default boolean supportsProvider(AIProvider provider) {
        return true; // By default, strategies support all providers
    }

    /**
     * Check if this strategy requires vision/image capabilities
     *
     * @return true if vision is required
     */
    default boolean requiresVision() {
        return getAnalysisType().requiresVisionModel();
    }
}
