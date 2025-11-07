package fr.postiqa.core.infrastructure.client.analysis.prompt;

import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.model.SocialPost;

import java.util.List;

/**
 * Interface for building analysis prompts with context
 */
public interface PromptBuilder {

    /**
     * Build prompt for analyzing a single post
     *
     * @param post Post to analyze
     * @return Built prompt string
     */
    String buildForSinglePost(SocialPost post);

    /**
     * Build prompt for analyzing multiple posts in batch
     *
     * @param posts Posts to analyze
     * @return Built prompt string
     */
    String buildForBatchPosts(List<SocialPost> posts);

    /**
     * Build prompt for analyzing a full profile
     *
     * @param posts All posts from the profile
     * @return Built prompt string
     */
    String buildForFullProfile(List<SocialPost> posts);

    /**
     * Build prompt with custom context
     *
     * @param content Content to analyze (text, image URL, etc.)
     * @param context Prompt context with variables
     * @return Built prompt string
     */
    String buildWithContext(String content, PromptContext context);

    /**
     * Get the appropriate template for a specific granularity
     *
     * @param granularity Analysis granularity level
     * @return PromptTemplate for the granularity
     */
    PromptTemplate getTemplateForGranularity(AnalysisGranularity granularity);

    /**
     * Get the system instruction (if any) to prepend to the prompt
     *
     * @return System instruction or null
     */
    default String getSystemInstruction() {
        return null;
    }
}
