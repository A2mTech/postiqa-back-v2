package fr.postiqa.core.domain.port;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.*;

import java.util.List;

/**
 * Port for AI-powered content analysis.
 * <p>
 * Defines the contract for analyzing social media content using external AI APIs.
 * Supports multiple analysis types, granularities, and AI providers.
 */
public interface AnalysisPort {

    /**
     * Analyze writing style of posts
     *
     * @param posts Posts to analyze
     * @param granularity Analysis granularity level
     * @param provider AI provider to use
     * @return Analysis result with writing style insights
     */
    AnalysisResult<WritingStyleAnalysis> analyzeWritingStyle(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    );

    /**
     * Analyze content themes and topics
     *
     * @param posts Posts to analyze
     * @param granularity Analysis granularity level
     * @param provider AI provider to use
     * @return Analysis result with content themes
     */
    AnalysisResult<ContentThemesAnalysis> analyzeContentThemes(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    );

    /**
     * Analyze images and visual content
     *
     * @param posts Posts with images to analyze
     * @param granularity Analysis granularity level
     * @param provider AI provider to use (must support vision)
     * @return Analysis result with image analysis
     */
    AnalysisResult<ImageAnalysisResult> analyzeImages(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    );

    /**
     * Analyze multimodal content (text + images)
     *
     * @param posts Posts with text and images to analyze
     * @param granularity Analysis granularity level
     * @param provider AI provider to use (must support multimodal)
     * @return Analysis result with multimodal analysis
     */
    AnalysisResult<MultimodalAnalysisResult> analyzeMultimodal(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    );

    /**
     * Generic analysis method that dispatches to specific analysis type
     *
     * @param posts Posts to analyze
     * @param analysisType Type of analysis to perform
     * @param granularity Analysis granularity level
     * @param provider AI provider to use
     * @return Analysis result (type depends on analysis type)
     */
    AnalysisResult<?> analyze(
        List<SocialPost> posts,
        AnalysisType analysisType,
        AnalysisGranularity granularity,
        AIProvider provider
    );

    /**
     * Legacy method for backward compatibility
     * Analyzes writing style from text strings
     *
     * @param posts Text content of posts
     * @return Simple writing profile
     * @deprecated Use analyzeWritingStyle(List<SocialPost>, AnalysisGranularity, AIProvider) instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default WritingProfile analyzeWritingStyle(List<String> posts) {
        throw new UnsupportedOperationException(
            "Legacy method not supported. Use analyzeWritingStyle(List<SocialPost>, AnalysisGranularity, AIProvider)"
        );
    }

    /**
     * Legacy writing profile record for backward compatibility
     *
     * @deprecated Use WritingStyleAnalysis instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    record WritingProfile(
        String tone,
        String narrativeStructure,
        List<String> commonHooks,
        String emojiUsage,
        String vocabulary
    ) {}
}
