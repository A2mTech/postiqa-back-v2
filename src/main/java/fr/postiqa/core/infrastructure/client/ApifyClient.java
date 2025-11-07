package fr.postiqa.core.infrastructure.client;

import fr.postiqa.core.infrastructure.client.dto.ApifyDatasetResponse;
import fr.postiqa.core.infrastructure.client.dto.ApifyRunResponse;
import fr.postiqa.core.infrastructure.client.dto.ApifyRunStatus;
import fr.postiqa.core.infrastructure.config.ApifyProperties;
import fr.postiqa.core.infrastructure.exception.JobNotFoundException;
import fr.postiqa.core.infrastructure.exception.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for Apify API
 * <p>
 * Handles all communication with Apify platform for scraping operations.
 */
@Component
public class ApifyClient {

    private static final Logger log = LoggerFactory.getLogger(ApifyClient.class);

    private final RestClient restClient;
    private final ApifyProperties properties;

    public ApifyClient(ApifyProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /**
     * Run actor synchronously and wait for completion (max 300 seconds)
     * Uses the /run-sync endpoint which blocks until the actor finishes or times out.
     *
     * @param actorId the actor ID to run
     * @param input   input parameters for the actor
     * @return the run response with dataset containing results
     */
    public ApifyRunResponse runSync(String actorId, Map<String, Object> input) {
        log.info("Running Apify actor synchronously: {}", actorId);

        try {
            return restClient.post()
                .uri("/v2/acts/{actorId}/run-sync", actorId)
                .body(input)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Client error: " + response.getStatusCode());
                })
                .onStatus(code -> code.value() == 408, (request, response) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Request timeout: Actor took more than 300 seconds");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Server error: " + response.getStatusCode());
                })
                .body(ApifyRunResponse.class);
        } catch (Exception e) {
            log.error("Failed to run actor synchronously: {}", actorId, e);
            throw new ProviderUnavailableException("Apify", "Failed to run sync", e);
        }
    }

    /**
     * Start a new actor run asynchronously
     *
     * @param actorId the actor ID to run
     * @param input   input parameters for the actor
     * @return the run response with run ID
     */
    public ApifyRunResponse startRun(String actorId, Map<String, Object> input) {
        log.info("Starting Apify run for actor: {}", actorId);

        try {
            return restClient.post()
                .uri("/v2/acts/{actorId}/runs", actorId)
                .body(input)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Client error: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Server error: " + response.getStatusCode());
                })
                .body(ApifyRunResponse.class);
        } catch (Exception e) {
            log.error("Failed to start Apify run for actor: {}", actorId, e);
            throw new ProviderUnavailableException("Apify", "Failed to start run", e);
        }
    }

    /**
     * Get the status of a running/completed actor run
     *
     * @param runId the run ID
     * @return the current run status
     */
    public ApifyRunStatus getRunStatus(String runId) {
        log.debug("Fetching Apify run status for: {}", runId);

        try {
            return restClient.get()
                .uri("/v2/actor-runs/{runId}", runId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        throw new JobNotFoundException(runId);
                    }
                    throw new ProviderUnavailableException("Apify",
                        "Client error: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Server error: " + response.getStatusCode());
                })
                .body(ApifyRunStatus.class);
        } catch (JobNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch run status for: {}", runId, e);
            throw new ProviderUnavailableException("Apify", "Failed to fetch run status", e);
        }
    }

    /**
     * Get the dataset items from a completed run
     *
     * @param runId the run ID
     * @return the dataset items
     */
    public List<Map<String, Object>> getRunDataset(String runId) {
        log.info("Fetching Apify dataset for run: {}", runId);

        try {
            ApifyDatasetResponse response = restClient.get()
                .uri("/v2/actor-runs/{runId}/dataset/items", runId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                    if (resp.getStatusCode().value() == 404) {
                        throw new JobNotFoundException(runId);
                    }
                    throw new ProviderUnavailableException("Apify",
                        "Client error: " + resp.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, resp) -> {
                    throw new ProviderUnavailableException("Apify",
                        "Server error: " + resp.getStatusCode());
                })
                .body(ApifyDatasetResponse.class);

            return response != null ? response.items() : List.of();
        } catch (JobNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch dataset for run: {}", runId, e);
            throw new ProviderUnavailableException("Apify", "Failed to fetch dataset", e);
        }
    }

    /**
     * Abort a running actor run
     *
     * @param runId the run ID to abort
     */
    public void abortRun(String runId) {
        log.info("Aborting Apify run: {}", runId);

        try {
            restClient.post()
                .uri("/v2/actor-runs/{runId}/abort", runId)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to abort run: {}", runId, e);
            throw new ProviderUnavailableException("Apify", "Failed to abort run", e);
        }
    }

    /**
     * Wait for a run to complete by polling its status
     *
     * @param runId        the run ID
     * @param timeout      maximum time to wait
     * @param pollInterval interval between status checks
     * @return the final run status
     */
    public ApifyRunStatus waitForCompletion(String runId, Duration timeout, Duration pollInterval) {
        log.info("Waiting for Apify run {} to complete (timeout: {})", runId, timeout);

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            ApifyRunStatus status = getRunStatus(runId);

            if (status.toJobStatus().isTerminal()) {
                log.info("Apify run {} completed with status: {}", runId, status.status());
                return status;
            }

            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProviderUnavailableException("Apify", "Polling interrupted", e);
            }
        }

        throw new ProviderUnavailableException("Apify",
            String.format("Run %s did not complete within %s", runId, timeout));
    }
}
