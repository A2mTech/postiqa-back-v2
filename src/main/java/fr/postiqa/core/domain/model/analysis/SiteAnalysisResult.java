package fr.postiqa.core.domain.model.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Complete site analysis result combining all aspects of website analysis.
 * Maps to Phase 2A in the ultra-deep analysis workflow.
 */
public record SiteAnalysisResult(
    String siteUrl,
    int totalPages,
    BusinessIdentity businessIdentity,
    ProductService productService,
    TargetAudience targetAudience,
    String businessModel,
    String stage,
    BrandIdentity brandIdentity,
    SocialProof socialProof,
    ContentStrategy contentStrategy,
    CTAsAnalysis ctasAnalysis,
    Map<String, Object> technicalStack,
    Map<String, Object> rawData
) {
    public SiteAnalysisResult {
        technicalStack = technicalStack != null ? Map.copyOf(technicalStack) : Collections.emptyMap();
        rawData = rawData != null ? Map.copyOf(rawData) : Collections.emptyMap();
    }

    public boolean hasBusinessIdentity() {
        return businessIdentity != null;
    }

    public boolean hasBrandIdentity() {
        return brandIdentity != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String siteUrl;
        private int totalPages;
        private BusinessIdentity businessIdentity;
        private ProductService productService;
        private TargetAudience targetAudience;
        private String businessModel;
        private String stage;
        private BrandIdentity brandIdentity;
        private SocialProof socialProof;
        private ContentStrategy contentStrategy;
        private CTAsAnalysis ctasAnalysis;
        private Map<String, Object> technicalStack = Collections.emptyMap();
        private Map<String, Object> rawData = Collections.emptyMap();

        public Builder siteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder businessIdentity(BusinessIdentity businessIdentity) {
            this.businessIdentity = businessIdentity;
            return this;
        }

        public Builder productService(ProductService productService) {
            this.productService = productService;
            return this;
        }

        public Builder targetAudience(TargetAudience targetAudience) {
            this.targetAudience = targetAudience;
            return this;
        }

        public Builder businessModel(String businessModel) {
            this.businessModel = businessModel;
            return this;
        }

        public Builder stage(String stage) {
            this.stage = stage;
            return this;
        }

        public Builder brandIdentity(BrandIdentity brandIdentity) {
            this.brandIdentity = brandIdentity;
            return this;
        }

        public Builder socialProof(SocialProof socialProof) {
            this.socialProof = socialProof;
            return this;
        }

        public Builder contentStrategy(ContentStrategy contentStrategy) {
            this.contentStrategy = contentStrategy;
            return this;
        }

        public Builder ctasAnalysis(CTAsAnalysis ctasAnalysis) {
            this.ctasAnalysis = ctasAnalysis;
            return this;
        }

        public Builder technicalStack(Map<String, Object> technicalStack) {
            this.technicalStack = technicalStack;
            return this;
        }

        public Builder rawData(Map<String, Object> rawData) {
            this.rawData = rawData;
            return this;
        }

        public SiteAnalysisResult build() {
            return new SiteAnalysisResult(
                siteUrl,
                totalPages,
                businessIdentity,
                productService,
                targetAudience,
                businessModel,
                stage,
                brandIdentity,
                socialProof,
                contentStrategy,
                ctasAnalysis,
                technicalStack,
                rawData
            );
        }
    }
}

/**
 * Product or service information
 */
record ProductService(
    String type,
    String category,
    String description,
    List<String> features,
    List<String> useCases
) {
    public ProductService {
        features = features != null ? List.copyOf(features) : Collections.emptyList();
        useCases = useCases != null ? List.copyOf(useCases) : Collections.emptyList();
    }
}

/**
 * Target audience information
 */
record TargetAudience(
    String primary,
    String secondary,
    List<String> industries,
    String companySize
) {
    public TargetAudience {
        industries = industries != null ? List.copyOf(industries) : Collections.emptyList();
    }
}

/**
 * Brand identity from website
 */
record BrandIdentity(
    List<String> tone,
    List<String> colorsDetected,
    String visualStyle
) {
    public BrandIdentity {
        tone = tone != null ? List.copyOf(tone) : Collections.emptyList();
        colorsDetected = colorsDetected != null ? List.copyOf(colorsDetected) : Collections.emptyList();
    }
}

/**
 * Social proof elements found on website
 */
record SocialProof(
    int testimonialsCount,
    List<String> caseStudies,
    List<String> clientLogos,
    List<String> statsMentioned,
    List<String> trustSignals
) {
    public SocialProof {
        caseStudies = caseStudies != null ? List.copyOf(caseStudies) : Collections.emptyList();
        clientLogos = clientLogos != null ? List.copyOf(clientLogos) : Collections.emptyList();
        statsMentioned = statsMentioned != null ? List.copyOf(statsMentioned) : Collections.emptyList();
        trustSignals = trustSignals != null ? List.copyOf(trustSignals) : Collections.emptyList();
    }
}

/**
 * Content strategy detected on website
 */
record ContentStrategy(
    boolean hasBlog,
    List<String> blogTopics,
    List<String> contentTypes,
    String postingFrequency
) {
    public ContentStrategy {
        blogTopics = blogTopics != null ? List.copyOf(blogTopics) : Collections.emptyList();
        contentTypes = contentTypes != null ? List.copyOf(contentTypes) : Collections.emptyList();
    }
}

/**
 * CTAs (Call-to-Actions) analysis
 */
record CTAsAnalysis(
    String primaryCta,
    List<String> secondaryCtAs,
    String conversionFocus
) {
    public CTAsAnalysis {
        secondaryCtAs = secondaryCtAs != null ? List.copyOf(secondaryCtAs) : Collections.emptyList();
    }
}
