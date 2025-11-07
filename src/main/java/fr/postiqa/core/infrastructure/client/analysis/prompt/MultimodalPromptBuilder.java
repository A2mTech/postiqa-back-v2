package fr.postiqa.core.infrastructure.client.analysis.prompt;

import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.model.SocialPost;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prompt builder for multimodal (text + image) analysis
 */
@Component
public class MultimodalPromptBuilder implements PromptBuilder {

    private static final String SYSTEM_INSTRUCTION = """
        You are an expert multimodal content analyst specializing in the relationship between text and visual content in social media.
        Analyze how text and images work together to create coherent, impactful messaging. Evaluate alignment, complementarity, and overall effectiveness.
        """;

    private static final PromptTemplate SINGLE_POST_TEMPLATE = PromptTemplate.of("""
        Analyze the relationship between text and visual content in this social media post:

        Platform: {{platform}}
        Text: {{textContent}}
        Image URL: {{imageUrl}}

        Provide a JSON response with the following structure:
        {
          "overallMessage": "the unified message conveyed by text and image together",
          "textImageAlignment": {
            "alignmentScore": score from 1-10,
            "alignmentQuality": "strong/moderate/weak",
            "explanation": "how well text and image work together",
            "complementary": true/false,
            "contradictory": true/false
          },
          "narrativeCoherence": "how the text and image tell a cohesive story",
          "emotionalImpact": "combined emotional effect of text and visuals",
          "keyTakeaways": ["main insights from the combined content"],
          "audienceEngagement": "predicted engagement level and why",
          "strengths": ["what works well in this text-image combination"],
          "improvementSuggestions": ["how to improve text-image synergy"]
        }
        """);

    private static final PromptTemplate BATCH_POSTS_TEMPLATE = PromptTemplate.of("""
        Analyze the multimodal content strategy across {{itemCount}} social media posts:

        Platform: {{platform}}
        Posts (text + images):
        {{content}}

        Identify patterns in how text and visuals are combined. Provide a JSON response:
        {
          "overallMessage": "consistent messaging strategy across posts",
          "textImageAlignment": {
            "alignmentScore": average alignment score 1-10,
            "alignmentQuality": "overall quality of text-image pairing",
            "explanation": "typical relationship between text and visuals",
            "complementary": typical pattern,
            "contradictory": false
          },
          "narrativeCoherence": "consistent narrative approach across multimodal content",
          "emotionalImpact": "typical emotional strategy using text+visuals",
          "keyTakeaways": ["patterns in combined content strategy"],
          "audienceEngagement": "engagement strategy through multimodal content",
          "strengths": ["consistent strengths in text-visual combinations"],
          "improvementSuggestions": ["strategic improvements for multimodal content"]
        }
        """);

    private static final PromptTemplate FULL_PROFILE_TEMPLATE = PromptTemplate.of("""
        Analyze the complete multimodal content strategy across {{itemCount}} posts from multiple platforms:

        Posts with text and images:
        {{content}}

        Create a comprehensive multimodal content strategy profile. Provide a JSON response:
        {
          "overallMessage": "signature messaging approach across all multimodal content",
          "textImageAlignment": {
            "alignmentScore": overall alignment mastery score 1-10,
            "alignmentQuality": "mature/developing alignment strategy",
            "explanation": "sophisticated understanding of text-visual synergy",
            "complementary": strategic use of complementary content,
            "contradictory": false
          },
          "narrativeCoherence": "signature multimodal storytelling style",
          "emotionalImpact": "emotional branding through combined text and visuals",
          "keyTakeaways": ["multimodal content strategy principles"],
          "audienceEngagement": "engagement playbook for multimodal posts",
          "strengths": ["signature strengths in multimodal content creation"],
          "improvementSuggestions": ["strategic growth opportunities"]
        }
        """);

    @Override
    public String buildForSinglePost(SocialPost post) {
        String imageUrl = post.mediaUrls() != null && !post.mediaUrls().isEmpty()
            ? post.mediaUrls().get(0)
            : "";

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.SINGLE_POST)
            .platform(post.platform())
            .variable("textContent", post.content() != null ? post.content() : "")
            .variable("imageUrl", imageUrl)
            .build();

        return SINGLE_POST_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildForBatchPosts(List<SocialPost> posts) {
        String formattedPosts = posts.stream()
            .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
            .map(post -> String.format("[%s]\nText: %s\nImage: %s",
                post.platform().getDisplayName(),
                post.content() != null ? post.content() : "",
                post.mediaUrls().get(0)))
            .collect(Collectors.joining("\n\n"));

        long postsWithMedia = posts.stream()
            .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
            .count();

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.BATCH_POSTS)
            .platform(posts.isEmpty() ? null : posts.get(0).platform())
            .itemCount((int) postsWithMedia)
            .variable("content", formattedPosts)
            .build();

        return BATCH_POSTS_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildForFullProfile(List<SocialPost> posts) {
        String formattedPosts = posts.stream()
            .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
            .map(post -> String.format("[%s - %s]\nText: %s\nImages: %s",
                post.platform().getDisplayName(),
                post.publishedAt(),
                post.content() != null ? post.content() : "",
                String.join(", ", post.mediaUrls())))
            .collect(Collectors.joining("\n\n"));

        long postsWithMedia = posts.stream()
            .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
            .count();

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.FULL_PROFILE)
            .itemCount((int) postsWithMedia)
            .variable("content", formattedPosts)
            .build();

        return FULL_PROFILE_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildWithContext(String content, PromptContext context) {
        Map<String, Object> variables = new HashMap<>(context.customVariables());

        // If content is provided as single string, try to split it intelligently
        if (!variables.containsKey("textContent") && !variables.containsKey("imageUrl")) {
            // Assume content is already formatted or handle as text
            variables.put("textContent", content);
        }

        PromptContext enrichedContext = PromptContext.builder()
            .granularity(context.granularity())
            .platform(context.platform())
            .itemCount(context.itemCount())
            .variables(variables)
            .build();

        return getTemplateForGranularity(context.granularity()).interpolate(enrichedContext);
    }

    @Override
    public PromptTemplate getTemplateForGranularity(AnalysisGranularity granularity) {
        return switch (granularity) {
            case SINGLE_POST -> SINGLE_POST_TEMPLATE;
            case BATCH_POSTS -> BATCH_POSTS_TEMPLATE;
            case FULL_PROFILE -> FULL_PROFILE_TEMPLATE;
        };
    }

    @Override
    public String getSystemInstruction() {
        return SYSTEM_INSTRUCTION;
    }
}
