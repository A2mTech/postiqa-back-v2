package fr.postiqa.core.infrastructure.client.analysis.prompt;

import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.model.SocialPost;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prompt builder for content themes and topics analysis
 */
@Component
public class ContentThemesPromptBuilder implements PromptBuilder {

    private static final String SYSTEM_INSTRUCTION = """
        You are an expert content strategist and thematic analyst. Analyze social media posts to extract main themes, topics, sentiments, and strategic content patterns.
        Identify what the author talks about, their expertise areas, messaging strategies, and audience engagement approaches.
        """;

    private static final PromptTemplate SINGLE_POST_TEMPLATE = PromptTemplate.of("""
        Analyze the themes and content strategy of the following social media post:

        Platform: {{platform}}
        Post: {{content}}

        Provide a JSON response with the following structure:
        {
          "mainThemes": ["primary topics covered in this post"],
          "secondaryThemes": ["supporting or related topics"],
          "overallSentiment": "the dominant sentiment (positive, neutral, negative, mixed)",
          "themeSentiments": {"theme1": "sentiment", "theme2": "sentiment"},
          "keyMessages": ["main points the author is communicating"],
          "callsToAction": ["any explicit or implicit CTAs"],
          "contentIntent": "the purpose of this post (educate, inspire, sell, engage, etc.)",
          "targetAudience": "who this content is aimed at",
          "expertiseAreas": ["areas of expertise demonstrated"]
        }
        """);

    private static final PromptTemplate BATCH_POSTS_TEMPLATE = PromptTemplate.of("""
        Analyze the recurring themes and content strategy across {{itemCount}} social media posts:

        Platform: {{platform}}
        Posts:
        {{content}}

        Identify patterns and provide a JSON response:
        {
          "mainThemes": ["most frequently discussed topics"],
          "secondaryThemes": ["recurring supporting topics"],
          "overallSentiment": "predominant sentiment across posts",
          "themeSentiments": {"theme1": "typical sentiment", "theme2": "typical sentiment"},
          "keyMessages": ["recurring messages and philosophies"],
          "callsToAction": ["common types of CTAs used"],
          "contentIntent": "primary content strategy and goals",
          "targetAudience": "primary audience profile",
          "expertiseAreas": ["demonstrated areas of expertise"],
          "themeFrequency": {"theme1": estimatedCount, "theme2": estimatedCount}
        }
        """);

    private static final PromptTemplate FULL_PROFILE_TEMPLATE = PromptTemplate.of("""
        Analyze the complete content strategy and thematic profile across {{itemCount}} posts from multiple platforms:

        Posts:
        {{content}}

        Create a comprehensive thematic analysis. Provide a JSON response:
        {
          "mainThemes": ["core themes that define the author's content"],
          "secondaryThemes": ["supporting themes and occasional topics"],
          "overallSentiment": "typical emotional tone with variations",
          "themeSentiments": {"theme": "sentiment analysis for each major theme"},
          "keyMessages": ["signature messages and thought leadership positions"],
          "callsToAction": ["CTA strategies and patterns"],
          "contentIntent": "overarching content strategy and mission",
          "targetAudience": "detailed audience profile based on content patterns",
          "expertiseAreas": ["validated areas of expertise and authority"],
          "themeFrequency": {"theme": frequency across all posts}
        }
        """);

    @Override
    public String buildForSinglePost(SocialPost post) {
        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.SINGLE_POST)
            .platform(post.platform())
            .variable("content", post.content())
            .build();

        return SINGLE_POST_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildForBatchPosts(List<SocialPost> posts) {
        String formattedPosts = posts.stream()
            .map(post -> String.format("[%s] %s", post.platform().getDisplayName(), post.content()))
            .collect(Collectors.joining("\n\n"));

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.BATCH_POSTS)
            .platform(posts.isEmpty() ? null : posts.get(0).platform())
            .itemCount(posts.size())
            .variable("content", formattedPosts)
            .build();

        return BATCH_POSTS_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildForFullProfile(List<SocialPost> posts) {
        String formattedPosts = posts.stream()
            .map(post -> String.format("[%s - %s] %s",
                post.platform().getDisplayName(),
                post.publishedAt(),
                post.content()))
            .collect(Collectors.joining("\n\n"));

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.FULL_PROFILE)
            .itemCount(posts.size())
            .variable("content", formattedPosts)
            .build();

        return FULL_PROFILE_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildWithContext(String content, PromptContext context) {
        Map<String, Object> variables = new HashMap<>(context.customVariables());
        variables.put("content", content);

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
