package fr.postiqa.core.adapter.out;

import fr.postiqa.core.domain.enums.JobStatus;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.ScrapingJobResult;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.domain.port.ScrapingPort;
import fr.postiqa.core.infrastructure.client.ApifyClient;
import fr.postiqa.core.infrastructure.client.actor.ActorConfig;
import fr.postiqa.core.infrastructure.client.actor.ActorInputBuilder;
import fr.postiqa.core.infrastructure.client.actor.ActorOutputParser;
import fr.postiqa.core.infrastructure.client.actor.ActorRegistry;
import fr.postiqa.core.infrastructure.client.dto.ApifyRunResponse;
import fr.postiqa.core.infrastructure.client.dto.ApifyRunStatus;
import fr.postiqa.core.infrastructure.config.ApifyProperties;
import fr.postiqa.core.infrastructure.exception.JobNotCompleteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Apify implementation of ScrapingPort using ActorRegistry
 * <p>
 * Dynamically selects the appropriate actor, input builder, and output parser
 * based on the platform and type of scraping operation.
 */
@Component
public class ApifyScrapingGateway implements ScrapingPort {

    private static final Logger log = LoggerFactory.getLogger(ApifyScrapingGateway.class);

    private final ApifyClient apifyClient;
    private final ApifyProperties properties;
    private final ActorRegistry actorRegistry;

    public ApifyScrapingGateway(
        ApifyClient apifyClient,
        ApifyProperties properties,
        ActorRegistry actorRegistry
    ) {
        this.apifyClient = apifyClient;
        this.properties = properties;
        this.actorRegistry = actorRegistry;
    }

    @Override
    public List<SocialPost> scrapePosts(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("Scraping posts from {} for user: {} (max: {})", platform, userId, maxPosts);

        // Get actor configuration
        ActorConfig config = actorRegistry.getActorOrThrow(platform, ActorConfig.ActorType.SOCIAL_POSTS);
        ActorInputBuilder inputBuilder = actorRegistry.getInputBuilder(config);
        ActorOutputParser outputParser = actorRegistry.getOutputParser(config);

        // Build input
        Map<String, Object> input = inputBuilder.buildPostsInput(userId, maxPosts);

        // Execute actor (sync or async based on config)
        ApifyRunResponse run;
        if (config.isSupportsSync()) {
            run = apifyClient.runSync(config.getActorId(), input);
        } else {
            run = apifyClient.startRun(config.getActorId(), input);
            ApifyRunStatus status = apifyClient.waitForCompletion(
                run.id(),
                config.getDefaultTimeout(),
                properties.getPollingInterval()
            );

            if (!status.toJobStatus().isSuccess()) {
                throw new RuntimeException("Scraping failed with status: " + status.status());
            }
        }

        // Fetch and parse results
        List<Map<String, Object>> items = apifyClient.getRunDataset(run.id());
        return outputParser.parsePosts(items);
    }

    @Override
    public SocialProfile scrapeProfile(SocialPlatform platform, String userId) {
        log.info("Scraping profile from {} for user: {}", platform, userId);

        // Get actor configuration
        ActorConfig config = actorRegistry.getActorOrThrow(platform, ActorConfig.ActorType.SOCIAL_PROFILE);
        ActorInputBuilder inputBuilder = actorRegistry.getInputBuilder(config);
        ActorOutputParser outputParser = actorRegistry.getOutputParser(config);

        // Build input
        Map<String, Object> input = inputBuilder.buildProfileInput(userId);

        // Execute actor (sync or async based on config)
        ApifyRunResponse run;
        if (config.isSupportsSync()) {
            run = apifyClient.runSync(config.getActorId(), input);
        } else {
            run = apifyClient.startRun(config.getActorId(), input);
            ApifyRunStatus status = apifyClient.waitForCompletion(
                run.id(),
                config.getDefaultTimeout(),
                properties.getPollingInterval()
            );

            if (!status.toJobStatus().isSuccess()) {
                throw new RuntimeException("Scraping failed with status: " + status.status());
            }
        }

        // Fetch and parse results
        List<Map<String, Object>> items = apifyClient.getRunDataset(run.id());
        if (items.isEmpty()) {
            throw new RuntimeException("No profile data found");
        }

        return outputParser.parseProfile(items.get(0));
    }

    @Override
    public String startAsyncPostsScraping(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("Starting async scraping of posts from {} for user: {}", platform, userId);

        ActorConfig config = actorRegistry.getActorOrThrow(platform, ActorConfig.ActorType.SOCIAL_POSTS);
        ActorInputBuilder inputBuilder = actorRegistry.getInputBuilder(config);

        Map<String, Object> input = inputBuilder.buildPostsInput(userId, maxPosts);
        ApifyRunResponse run = apifyClient.startRun(config.getActorId(), input);

        return run.id();
    }

    @Override
    public String startAsyncProfileScraping(SocialPlatform platform, String userId) {
        log.info("Starting async scraping of profile from {} for user: {}", platform, userId);

        ActorConfig config = actorRegistry.getActorOrThrow(platform, ActorConfig.ActorType.SOCIAL_PROFILE);
        ActorInputBuilder inputBuilder = actorRegistry.getInputBuilder(config);

        Map<String, Object> input = inputBuilder.buildProfileInput(userId);
        ApifyRunResponse run = apifyClient.startRun(config.getActorId(), input);

        return run.id();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ScrapingJobResult<T> pollJob(String jobId) {
        log.debug("Polling job: {}", jobId);

        ApifyRunStatus status = apifyClient.getRunStatus(jobId);
        JobStatus jobStatus = status.toJobStatus();

        if (!jobStatus.isTerminal()) {
            return (ScrapingJobResult<T>) ScrapingJobResult.running(jobId, status.startedAt());
        }

        if (jobStatus.isFailure()) {
            String errorMessage = status.exitCode() != null && status.exitCode() != 0
                ? "Exit code: " + status.exitCode()
                : "Unknown error";
            return (ScrapingJobResult<T>) ScrapingJobResult.failure(
                jobId, errorMessage, status.startedAt(), status.finishedAt()
            );
        }

        // Success - don't fetch data in poll, just return status
        return (ScrapingJobResult<T>) new ScrapingJobResult<>(
            jobId,
            jobStatus,
            null, // Result fetched separately via getJobResult
            null,
            status.startedAt(),
            status.finishedAt(),
            Map.of("datasetId", status.defaultDatasetId())
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getJobResult(String jobId, Class<T> resultType) {
        log.info("Fetching result for job: {}", jobId);

        ApifyRunStatus status = apifyClient.getRunStatus(jobId);
        JobStatus jobStatus = status.toJobStatus();

        if (!jobStatus.isSuccess()) {
            throw new JobNotCompleteException(jobId, jobStatus);
        }

        List<Map<String, Object>> items = apifyClient.getRunDataset(jobId);

        // Note: In production, platform info should be stored in job metadata
        // For now, try to infer or use a generic parser
        if (resultType.isAssignableFrom(List.class)) {
            // Assume List<SocialPost> - use generic parser as fallback
            ActorOutputParser parser = new fr.postiqa.core.infrastructure.client.actor.impl.GenericActorOutputParser();
            return (T) parser.parsePosts(items);
        } else if (resultType.equals(SocialProfile.class)) {
            if (items.isEmpty()) {
                throw new RuntimeException("No profile data found in job result");
            }
            ActorOutputParser parser = new fr.postiqa.core.infrastructure.client.actor.impl.GenericActorOutputParser();
            return (T) parser.parseProfile(items.get(0));
        }

        throw new IllegalArgumentException("Unsupported result type: " + resultType);
    }

    @Override
    public void cancelJob(String jobId) {
        log.info("Cancelling job: {}", jobId);
        apifyClient.abortRun(jobId);
    }
}
