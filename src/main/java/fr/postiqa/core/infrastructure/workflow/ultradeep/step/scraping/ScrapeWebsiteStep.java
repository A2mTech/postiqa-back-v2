package fr.postiqa.core.infrastructure.workflow.ultradeep.step.scraping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.model.WebsiteContent;
import fr.postiqa.core.domain.port.WebScrapingPort;
import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Step 1.1: Scrape website data using Apify.
 * <p>
 * Extracts: business identity, products/services, target audience, brand identity,
 * social proof, content strategy, CTAs, technical stack.
 */
@Slf4j
@Component
public class ScrapeWebsiteStep implements WorkflowStep<Void, Map<String, Object>> {

    private final WebScrapingPort webScrapingPort;
    private final ObjectMapper objectMapper;

    public ScrapeWebsiteStep(WebScrapingPort webScrapingPort, ObjectMapper objectMapper) {
        this.webScrapingPort = webScrapingPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStepId() {
        return "scrape-website";
    }

    @Override
    public String getStepName() {
        return "Scrape Website Data";
    }

    @Override
    public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
        String websiteUrl = context.getRequired("website_url", String.class);
        log.info("Scraping website: {}", websiteUrl);

        // Call WebScrapingPort to scrape the website
        WebsiteContent websiteContent = webScrapingPort.scrapeWebsite(websiteUrl, ContentType.ABOUT_PAGE);

        // Convert to Map for context storage
        @SuppressWarnings("unchecked")
        Map<String, Object> websiteData = objectMapper.convertValue(websiteContent, Map.class);

        log.info("Website scraping completed for: {}", websiteUrl);

        return websiteData;
    }

    @Override
    public Optional<String> getOutputKey() {
        return Optional.of("website_data");
    }

    @Override
    public Duration getTimeout() {
        return Duration.ofMinutes(10);
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RetryPolicy.exponentialBackoff(3, Duration.ofSeconds(5));
    }

    @Override
    public void onBeforeExecute(Void input, WorkflowContext context) {
        String websiteUrl = context.get("website_url", String.class).orElse("unknown");
        log.info("[SCRAPING] Starting website scraping for: {}", websiteUrl);
    }

    @Override
    public void onAfterExecute(Map<String, Object> output, WorkflowContext context) {
        log.info("[SCRAPING] Website scraping successful. Data keys: {}", output.keySet());
    }

    @Override
    public void onError(Exception error, WorkflowContext context) {
        String websiteUrl = context.get("website_url", String.class).orElse("unknown");
        log.error("[SCRAPING] Website scraping failed for: {}. Error: {}",
            websiteUrl, error.getMessage());
    }
}
