package fr.postiqa.core.infrastructure.client.analysis.prompt;

import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.model.SocialPost;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prompt builder for writing style analysis
 */
@Component
public class WritingStylePromptBuilder implements PromptBuilder {

    private static final String SYSTEM_INSTRUCTION = """
        You are an expert writing style analyst. Analyze social media posts to identify unique writing patterns, tone, vocabulary, and narrative structures.
        Provide detailed, actionable insights about the author's writing style that can be used to replicate their voice authentically.
        """;

    private static final PromptTemplate SINGLE_POST_TEMPLATE = PromptTemplate.of("""
        Analyze the following social media post and identify the writing style characteristics:

        Platform: {{platform}}
        Post: {{content}}

        Provide a JSON response with the following structure:
        {
          "tone": "brief description of the tone (e.g., professional, casual, inspirational)",
          "toneDescription": "detailed explanation of the tone nuances",
          "narrativeStructure": "how the post is structured (e.g., hook-story-lesson, direct statement, question-answer)",
          "commonHooks": ["list of attention-grabbing opening techniques used"],
          "emojiUsage": "description of how emojis are used (frequency, placement, types)",
          "vocabulary": "vocabulary level and word choice patterns (e.g., technical, simple, metaphorical)",
          "frequentPhrases": ["characteristic phrases or expressions"],
          "punctuationStyle": "punctuation patterns (e.g., frequent ellipsis, short sentences, exclamations)",
          "paragraphStructure": "how content is broken into paragraphs",
          "averagePostLength": estimated word count
        }
        """);

    private static final PromptTemplate BATCH_POSTS_TEMPLATE = PromptTemplate.of("""
        Analyze the following {{itemCount}} social media posts to identify consistent writing style patterns:

        Platform: {{platform}}
        Posts:
        {{content}}

        Identify patterns across all posts and provide a JSON response with this structure:
        {
          "tone": "predominant tone across posts",
          "toneDescription": "detailed explanation with examples from multiple posts",
          "narrativeStructure": "most common narrative structures used",
          "commonHooks": ["recurring attention-grabbing techniques"],
          "emojiUsage": "emoji usage patterns across posts",
          "vocabulary": "consistent vocabulary patterns",
          "frequentPhrases": ["phrases that appear in multiple posts"],
          "punctuationStyle": "consistent punctuation patterns",
          "paragraphStructure": "typical paragraph structure",
          "averagePostLength": average word count across posts
        }
        """);

    private static final PromptTemplate FULL_PROFILE_TEMPLATE = PromptTemplate.of("""
        Analyze the complete writing profile based on {{itemCount}} posts across multiple platforms:

        Posts:
        {{content}}

        Create a comprehensive writing style profile identifying the author's unique voice. Provide a JSON response:
        {
          "tone": "overall tone with variations noted",
          "toneDescription": "comprehensive tone analysis with cross-platform insights",
          "narrativeStructure": "preferred narrative structures ranked by frequency",
          "commonHooks": ["signature opening techniques used consistently"],
          "emojiUsage": "emoji usage philosophy and patterns",
          "vocabulary": "vocabulary profile with distinctive word choices",
          "frequentPhrases": ["signature phrases and expressions"],
          "punctuationStyle": "punctuation personality and patterns",
          "paragraphStructure": "structural preferences and variations",
          "averagePostLength": overall average with platform-specific notes
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
