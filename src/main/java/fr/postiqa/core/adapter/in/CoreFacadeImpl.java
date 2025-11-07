package fr.postiqa.core.adapter.in;

import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.ScrapingJobResult;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.domain.model.WebsiteContent;
import fr.postiqa.core.usecase.GetSocialPostsUseCase;
import fr.postiqa.core.usecase.GetSocialProfileUseCase;
import fr.postiqa.core.usecase.GetWebsiteDataUseCase;
import fr.postiqa.core.usecase.PollScrapingJobUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of CoreFacade
 * <p>
 * Delegates to use cases and provides a clean API for business/agency modules.
 */
@Component
public class CoreFacadeImpl implements CoreFacade {

    private static final Logger log = LoggerFactory.getLogger(CoreFacadeImpl.class);

    private final GetSocialPostsUseCase getSocialPostsUseCase;
    private final GetSocialProfileUseCase getSocialProfileUseCase;
    private final GetWebsiteDataUseCase getWebsiteDataUseCase;
    private final PollScrapingJobUseCase pollScrapingJobUseCase;

    public CoreFacadeImpl(
        GetSocialPostsUseCase getSocialPostsUseCase,
        GetSocialProfileUseCase getSocialProfileUseCase,
        GetWebsiteDataUseCase getWebsiteDataUseCase,
        PollScrapingJobUseCase pollScrapingJobUseCase
    ) {
        this.getSocialPostsUseCase = getSocialPostsUseCase;
        this.getSocialProfileUseCase = getSocialProfileUseCase;
        this.getWebsiteDataUseCase = getWebsiteDataUseCase;
        this.pollScrapingJobUseCase = pollScrapingJobUseCase;
    }

    // ========== SYNCHRONOUS METHODS ==========

    @Override
    public List<SocialPost> getSocialPosts(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("CoreFacade: getSocialPosts({}, {}, {})", platform, userId, maxPosts);
        return getSocialPostsUseCase.execute(platform, userId, maxPosts);
    }

    @Override
    public SocialProfile getSocialProfile(SocialPlatform platform, String userId) {
        log.info("CoreFacade: getSocialProfile({}, {})", platform, userId);
        return getSocialProfileUseCase.execute(platform, userId);
    }

    @Override
    public WebsiteContent getWebsiteData(String url, ContentType contentType) {
        log.info("CoreFacade: getWebsiteData({}, {})", url, contentType);
        return getWebsiteDataUseCase.execute(url, contentType);
    }

    // ========== ASYNC THREAD METHODS ==========

    @Override
    public CompletableFuture<List<SocialPost>> getSocialPostsAsync(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("CoreFacade: getSocialPostsAsync({}, {}, {})", platform, userId, maxPosts);
        return getSocialPostsUseCase.executeAsync(platform, userId, maxPosts);
    }

    @Override
    public CompletableFuture<SocialProfile> getSocialProfileAsync(SocialPlatform platform, String userId) {
        log.info("CoreFacade: getSocialProfileAsync({}, {})", platform, userId);
        return getSocialProfileUseCase.executeAsync(platform, userId);
    }

    @Override
    public CompletableFuture<WebsiteContent> getWebsiteDataAsync(String url, ContentType contentType) {
        log.info("CoreFacade: getWebsiteDataAsync({}, {})", url, contentType);
        return getWebsiteDataUseCase.executeAsync(url, contentType);
    }

    // ========== ASYNC NATIVE METHODS ==========

    @Override
    public String startSocialPostsJob(SocialPlatform platform, String userId, Integer maxPosts) {
        log.info("CoreFacade: startSocialPostsJob({}, {}, {})", platform, userId, maxPosts);
        return getSocialPostsUseCase.startJob(platform, userId, maxPosts);
    }

    @Override
    public String startSocialProfileJob(SocialPlatform platform, String userId) {
        log.info("CoreFacade: startSocialProfileJob({}, {})", platform, userId);
        return getSocialProfileUseCase.startJob(platform, userId);
    }

    @Override
    public String startWebsiteDataJob(String url, ContentType contentType) {
        log.info("CoreFacade: startWebsiteDataJob({}, {})", url, contentType);
        return getWebsiteDataUseCase.startJob(url, contentType);
    }

    @Override
    public <T> ScrapingJobResult<T> pollJob(String jobId) {
        log.debug("CoreFacade: pollJob({})", jobId);
        return pollScrapingJobUseCase.pollJob(jobId);
    }

    @Override
    public <T> T getJobResult(String jobId, Class<T> resultType) {
        log.info("CoreFacade: getJobResult({}, {})", jobId, resultType.getSimpleName());
        return pollScrapingJobUseCase.getSocialJobResult(jobId, resultType);
    }

    @Override
    public void cancelJob(String jobId) {
        log.info("CoreFacade: cancelJob({})", jobId);
        pollScrapingJobUseCase.cancelSocialJob(jobId);
    }
}
