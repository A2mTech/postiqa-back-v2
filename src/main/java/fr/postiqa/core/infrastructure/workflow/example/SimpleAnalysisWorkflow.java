package fr.postiqa.core.infrastructure.workflow.example;

import fr.postiqa.core.domain.workflow.enums.CompensationStrategy;
import fr.postiqa.core.domain.workflow.enums.ExecutionMode;
import fr.postiqa.core.domain.workflow.model.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Example workflow demonstrating the framework usage.
 * This workflow performs a simple 3-step analysis process:
 * 1. Scrape data
 * 2. Analyze data
 * 3. Generate report
 *
 * Usage example for implementing complex analysis workflows (Phase 1-4 from requirements).
 */
@Slf4j
public class SimpleAnalysisWorkflow {

    /**
     * Build a simple analysis workflow
     */
    public static WorkflowDefinition buildWorkflow() {
        return WorkflowDefinition.builder()
            .name("SimpleAnalysisWorkflow")
            .description("Example workflow: scrape → analyze → report")
            .executionMode(ExecutionMode.SEQUENTIAL)
            .compensationStrategy(CompensationStrategy.REVERSE_ORDER)
            .globalTimeout(Duration.ofMinutes(30))
            // Step 1: Scrape data
            .addStep(new ScrapeDataStep())
            // Step 2: Analyze scraped data (depends on step 1)
            .addStep(new AnalyzeDataStep(), "scrape-data")
            // Step 3: Generate report (depends on step 2)
            .addStep(new GenerateReportStep(), "analyze-data")
            .build();
    }

    /**
     * Example Step 1: Scrape Data
     */
    static class ScrapeDataStep implements WorkflowStep<Void, Map<String, Object>> {

        @Override
        public String getStepId() {
            return "scrape-data";
        }

        @Override
        public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
            log.info("Scraping data...");

            // Simulate scraping delay
            Thread.sleep(1000);

            // Return scraped data
            return Map.of(
                "site_url", "https://example.com",
                "pages_scraped", 10,
                "content", "Sample content..."
            );
        }

        @Override
        public Optional<String> getOutputKey() {
            return Optional.of("scraped_data"); // Store output in context
        }

        @Override
        public Duration getTimeout() {
            return Duration.ofMinutes(5);
        }

        @Override
        public RetryPolicy getRetryPolicy() {
            return RetryPolicy.exponentialBackoff(3, Duration.ofSeconds(1));
        }

        @Override
        public Optional<CompensationAction<Map<String, Object>>> getCompensationAction() {
            return Optional.of((data, ctx) -> {
                log.info("Compensating scrape: cleaning up scraped data");
                // Cleanup logic here
            });
        }
    }

    /**
     * Example Step 2: Analyze Data
     */
    static class AnalyzeDataStep implements WorkflowStep<Void, Map<String, Object>> {

        @Override
        public String getStepId() {
            return "analyze-data";
        }

        @Override
        public Map<String, Object> execute(Void input, WorkflowContext context) throws Exception {
            log.info("Analyzing data...");

            // Get scraped data from context
            Map<String, Object> scrapedData = context.getRequired("scraped_data", Map.class);
            log.info("Analyzing {} pages", scrapedData.get("pages_scraped"));

            // Simulate analysis delay
            Thread.sleep(2000);

            // Return analysis results
            return Map.of(
                "sentiment", "positive",
                "key_topics", List.of("technology", "innovation"),
                "confidence_score", 0.95
            );
        }

        @Override
        public Optional<String> getOutputKey() {
            return Optional.of("analysis_results");
        }

        @Override
        public Duration getTimeout() {
            return Duration.ofMinutes(10);
        }

        @Override
        public RetryPolicy getRetryPolicy() {
            return RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(2));
        }
    }

    /**
     * Example Step 3: Generate Report
     */
    static class GenerateReportStep implements WorkflowStep<Void, String> {

        @Override
        public String getStepId() {
            return "generate-report";
        }

        @Override
        public String execute(Void input, WorkflowContext context) throws Exception {
            log.info("Generating report...");

            // Get analysis results from context
            Map<String, Object> analysisResults = context.getRequired("analysis_results", Map.class);

            // Simulate report generation
            Thread.sleep(500);

            String report = String.format(
                "Analysis Report\n" +
                "===============\n" +
                "Sentiment: %s\n" +
                "Topics: %s\n" +
                "Confidence: %.2f\n",
                analysisResults.get("sentiment"),
                analysisResults.get("key_topics"),
                analysisResults.get("confidence_score")
            );

            log.info("Report generated successfully");
            return report;
        }

        @Override
        public Optional<String> getOutputKey() {
            return Optional.of("final_report");
        }

        @Override
        public Duration getTimeout() {
            return Duration.ofMinutes(2);
        }
    }

    /**
     * Demonstrates parallel execution with the framework
     */
    public static WorkflowDefinition buildParallelWorkflow() {
        return WorkflowDefinition.builder()
            .name("ParallelAnalysisWorkflow")
            .description("Parallel execution example")
            .executionMode(ExecutionMode.PARALLEL) // Enable parallel execution
            .compensationStrategy(CompensationStrategy.REVERSE_ORDER)
            .globalTimeout(Duration.ofHours(1))
            // Root steps (no dependencies) - execute in parallel
            .addStep(new ScrapeWebsiteStep())
            .addStep(new ScrapeLinkedInStep())
            .addStep(new ScrapeTwitterStep())
            // Analysis step depends on all scraping steps - waits for all
            .addStep(new AggregateAnalysisStep(), "scrape-website", "scrape-linkedin", "scrape-twitter")
            .build();
    }

    // Dummy steps for parallel example
    static class ScrapeWebsiteStep implements WorkflowStep<Void, String> {
        @Override public String getStepId() { return "scrape-website"; }
        @Override public String execute(Void input, WorkflowContext context) {
            log.info("Scraping website...");
            return "website_data";
        }
        @Override public Optional<String> getOutputKey() { return Optional.of("website_data"); }
    }

    static class ScrapeLinkedInStep implements WorkflowStep<Void, String> {
        @Override public String getStepId() { return "scrape-linkedin"; }
        @Override public String execute(Void input, WorkflowContext context) {
            log.info("Scraping LinkedIn...");
            return "linkedin_data";
        }
        @Override public Optional<String> getOutputKey() { return Optional.of("linkedin_data"); }
    }

    static class ScrapeTwitterStep implements WorkflowStep<Void, String> {
        @Override public String getStepId() { return "scrape-twitter"; }
        @Override public String execute(Void input, WorkflowContext context) {
            log.info("Scraping Twitter...");
            return "twitter_data";
        }
        @Override public Optional<String> getOutputKey() { return Optional.of("twitter_data"); }
    }

    static class AggregateAnalysisStep implements WorkflowStep<Void, Map<String, Object>> {
        @Override public String getStepId() { return "aggregate-analysis"; }
        @Override public Map<String, Object> execute(Void input, WorkflowContext context) {
            log.info("Aggregating all data sources...");
            // Access data from all previous steps
            String website = context.getRequired("website_data", String.class);
            String linkedin = context.getRequired("linkedin_data", String.class);
            String twitter = context.getRequired("twitter_data", String.class);

            return Map.of(
                "sources_analyzed", 3,
                "aggregated_insights", "Combined analysis results"
            );
        }
        @Override public Optional<String> getOutputKey() { return Optional.of("aggregated_results"); }
    }
}
