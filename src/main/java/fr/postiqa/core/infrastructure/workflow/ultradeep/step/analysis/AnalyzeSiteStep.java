package fr.postiqa.core.infrastructure.workflow.ultradeep.step.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.adapter.out.analysis.JpaAnalysisResultsAdapter;
import fr.postiqa.core.domain.model.analysis.SiteAnalysisResult;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import fr.postiqa.core.infrastructure.client.OpenAIClient;
import fr.postiqa.database.entity.SiteAnalysisEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Step 2A: Analyze website data to extract business identity, brand, offerings, etc.
 * <p>
 * Uses LLM to analyze scraped website data and extract structured insights.
 */
@Slf4j
@Component
public class AnalyzeSiteStep implements WorkflowStep<Void, SiteAnalysisResult> {

    private final OpenAIClient openAIClient;
    private final JpaAnalysisResultsAdapter analysisAdapter;
    private final ObjectMapper objectMapper;

    public AnalyzeSiteStep(
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
        return "analyze-site";
    }

    @Override
    public String getStepName() {
        return "Analyze Website";
    }

    @Override
    public SiteAnalysisResult execute(Void input, WorkflowContext context) throws Exception {
        UUID userProfileAnalysisId = context.getRequired("user_profile_analysis_id", UUID.class);
        Map<String, Object> websiteData = context.getRequired("website_data", Map.class);

        log.info("Analyzing website data...");

        // Build comprehensive prompt for site analysis
        String prompt = buildSiteAnalysisPrompt(websiteData);
        String systemInstruction = """
            You are an expert business analyst. Analyze the website data and extract:
            - Business identity (name, tagline, value proposition, elevator pitch)
            - Products and services offered
            - Target audience and positioning
            - Business model and stage
            - Brand identity (visual, tone, values, personality)
            - Social proof (testimonials, case studies, metrics)
            - Content strategy (themes, formats, messaging)
            - CTAs and conversion strategy
            - Technical stack

            Return a structured JSON response.
            """;

        String response = openAIClient.complete(systemInstruction, prompt);
        SiteAnalysisResult siteAnalysis = parseSiteAnalysisResponse(response);

        // Save to database
        SiteAnalysisEntity entity = mapToEntity(siteAnalysis, userProfileAnalysisId);
        UUID siteAnalysisId = analysisAdapter.saveSiteAnalysis(userProfileAnalysisId, entity);

        log.info("Site analysis completed and saved. ID: {}", siteAnalysisId);

        return siteAnalysis;
    }

    private String buildSiteAnalysisPrompt(Map<String, Object> websiteData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following website data and provide a comprehensive business analysis:\n\n");

        // Include relevant website data fields
        if (websiteData.containsKey("home_page_content")) {
            prompt.append("Home Page:\n").append(websiteData.get("home_page_content")).append("\n\n");
        }
        if (websiteData.containsKey("about_page_content")) {
            prompt.append("About Page:\n").append(websiteData.get("about_page_content")).append("\n\n");
        }
        if (websiteData.containsKey("services_content")) {
            prompt.append("Services:\n").append(websiteData.get("services_content")).append("\n\n");
        }

        prompt.append("\nProvide your analysis in JSON format with the structure defined in the system instruction.");

        return prompt.toString();
    }

    private SiteAnalysisResult parseSiteAnalysisResponse(String response) throws Exception {
        // Remove markdown code blocks if present
        String jsonString = response.trim();
        if (jsonString.startsWith("```json")) {
            jsonString = jsonString.substring(7);
        } else if (jsonString.startsWith("```")) {
            jsonString = jsonString.substring(3);
        }
        if (jsonString.endsWith("```")) {
            jsonString = jsonString.substring(0, jsonString.length() - 3);
        }
        jsonString = jsonString.trim();

        // Parse JSON to SiteAnalysisResult
        return objectMapper.readValue(jsonString, SiteAnalysisResult.class);
    }

    private SiteAnalysisEntity mapToEntity(SiteAnalysisResult result, UUID userProfileAnalysisId) {
        SiteAnalysisEntity entity = new SiteAnalysisEntity();
        entity.setBusinessIdentity(objectMapper.convertValue(result.businessIdentity(), Map.class));
        entity.setProductService(objectMapper.convertValue(result.productService(), Map.class));
        entity.setTargetAudience(objectMapper.convertValue(result.targetAudience(), Map.class));
        entity.setBusinessModel(Map.of("model", result.businessModel()));
        entity.setStage(Map.of("stage", result.stage()));
        entity.setBrandIdentity(objectMapper.convertValue(result.brandIdentity(), Map.class));
        entity.setSocialProof(objectMapper.convertValue(result.socialProof(), Map.class));
        entity.setContentStrategy(objectMapper.convertValue(result.contentStrategy(), Map.class));
        entity.setCtasAnalysis(objectMapper.convertValue(result.ctasAnalysis(), Map.class));
        entity.setTechnicalStack(result.technicalStack());
        return entity;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("site_analysis_result");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(5);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(3));
    }

    @Override
    public boolean shouldSkip(WorkflowContext context) {
        // Skip if no website data was scraped
        return context.get("website_data", Map.class).isEmpty();
    }
}
