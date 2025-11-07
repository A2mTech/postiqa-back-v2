package fr.postiqa.core.infrastructure.client.analysis.prompt;

import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.model.SocialPost;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prompt builder for image and visual content analysis
 */
@Component
public class ImageAnalysisPromptBuilder implements PromptBuilder {

    private static final String SYSTEM_INSTRUCTION = """
        You are an expert visual content analyst specializing in social media imagery. Analyze images to identify visual elements, composition, branding, emotional impact, and strategic usage.
        Provide insights that help understand the visual content strategy and brand identity.
        """;

    private static final PromptTemplate SINGLE_IMAGE_TEMPLATE = PromptTemplate.of("""
        Analyze the following social media image in detail:

        Platform: {{platform}}
        Image URL: {{imageUrl}}
        Context (if any): {{textContext}}

        Provide a JSON response with the following structure:
        {
          "visualDescription": "detailed description of what's in the image",
          "detectedObjects": ["main objects, people, or elements visible"],
          "colors": ["dominant colors used"],
          "composition": "layout and composition style (rule of thirds, centered, minimalist, etc.)",
          "style": "visual style (professional, casual, artistic, minimalist, etc.)",
          "brandingElements": {
            "hasLogo": true/false,
            "brandColors": ["brand colors if detected"],
            "brandStyle": "description of branding consistency",
            "consistency": "how well branding is maintained"
          },
          "emotionalImpact": "emotional response the image likely evokes",
          "targetContext": "where/how this image should be used",
          "suggestedText": ["text captions that would complement this image"]
        }
        """);

    private static final PromptTemplate BATCH_IMAGES_TEMPLATE = PromptTemplate.of("""
        Analyze the visual content strategy across {{itemCount}} social media images:

        Platform: {{platform}}
        Images: {{imageUrls}}

        Identify visual patterns and branding consistency. Provide a JSON response:
        {
          "visualDescription": "overall visual style and themes",
          "detectedObjects": ["commonly featured objects and elements"],
          "colors": ["recurring color palette"],
          "composition": "typical composition patterns",
          "style": "consistent visual style",
          "brandingElements": {
            "hasLogo": consistency of logo usage,
            "brandColors": ["established brand colors"],
            "brandStyle": "overall brand visual identity",
            "consistency": "visual consistency score and description"
          },
          "emotionalImpact": "typical emotional tone of visuals",
          "targetContext": "strategic use of visuals",
          "suggestedText": ["types of captions that work well with these visuals"]
        }
        """);

    private static final PromptTemplate FULL_PROFILE_TEMPLATE = PromptTemplate.of("""
        Analyze the complete visual branding strategy across {{itemCount}} images from multiple platforms:

        Images with context:
        {{content}}

        Create a comprehensive visual content profile. Provide a JSON response:
        {
          "visualDescription": "signature visual style and evolution",
          "detectedObjects": ["signature visual elements and themes"],
          "colors": ["brand color palette with usage patterns"],
          "composition": "preferred composition styles and variations",
          "style": "defined visual brand identity",
          "brandingElements": {
            "hasLogo": "logo usage strategy",
            "brandColors": ["core brand colors"],
            "brandStyle": "comprehensive brand visual guidelines implied",
            "consistency": "cross-platform visual consistency analysis"
          },
          "emotionalImpact": "emotional branding strategy through visuals",
          "targetContext": "strategic visual content playbook",
          "suggestedText": ["signature caption styles that complement visuals"]
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
            .variable("imageUrl", imageUrl)
            .variable("textContext", post.content() != null ? post.content() : "")
            .build();

        return SINGLE_IMAGE_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildForBatchPosts(List<SocialPost> posts) {
        List<String> imageUrls = posts.stream()
            .flatMap(post -> post.mediaUrls() != null ? post.mediaUrls().stream() : java.util.stream.Stream.empty())
            .collect(Collectors.toList());

        String formattedUrls = String.join("\n", imageUrls);

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.BATCH_POSTS)
            .platform(posts.isEmpty() ? null : posts.get(0).platform())
            .itemCount(imageUrls.size())
            .variable("imageUrls", formattedUrls)
            .build();

        return BATCH_IMAGES_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildForFullProfile(List<SocialPost> posts) {
        String formattedContent = posts.stream()
            .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
            .map(post -> String.format("[%s - %s]\nImages: %s\nContext: %s",
                post.platform().getDisplayName(),
                post.publishedAt(),
                String.join(", ", post.mediaUrls()),
                post.content() != null ? post.content() : ""))
            .collect(Collectors.joining("\n\n"));

        long imageCount = posts.stream()
            .filter(post -> post.mediaUrls() != null && !post.mediaUrls().isEmpty())
            .mapToLong(post -> post.mediaUrls().size())
            .sum();

        PromptContext context = PromptContext.builder()
            .granularity(AnalysisGranularity.FULL_PROFILE)
            .itemCount((int) imageCount)
            .variable("content", formattedContent)
            .build();

        return FULL_PROFILE_TEMPLATE.interpolate(context);
    }

    @Override
    public String buildWithContext(String content, PromptContext context) {
        Map<String, Object> variables = new HashMap<>(context.customVariables());
        if (!variables.containsKey("imageUrl") && !variables.containsKey("imageUrls")) {
            variables.put("imageUrl", content);
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
            case SINGLE_POST -> SINGLE_IMAGE_TEMPLATE;
            case BATCH_POSTS -> BATCH_IMAGES_TEMPLATE;
            case FULL_PROFILE -> FULL_PROFILE_TEMPLATE;
        };
    }

    @Override
    public String getSystemInstruction() {
        return SYSTEM_INSTRUCTION;
    }
}
