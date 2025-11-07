package fr.postiqa.core.infrastructure.config;

import fr.postiqa.core.domain.enums.SocialPlatform;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Apify integration
 */
@Component
@ConfigurationProperties(prefix = "apify")
public class ApifyProperties {

    private String apiKey;
    private String baseUrl = "https://api.apify.com";
    private Duration defaultTimeout = Duration.ofMinutes(5);
    private Duration pollingInterval = Duration.ofSeconds(5);
    private int maxRetries = 3;

    /**
     * Actor IDs for each social platform scraping
     */
    private Map<SocialPlatform, String> socialActors = new HashMap<>();

    /**
     * Actor IDs for web scraping by content type
     */
    private Map<String, String> webActors = new HashMap<>();

    public ApifyProperties() {
        // Default actor IDs (to be overridden in application.properties)
        socialActors.put(SocialPlatform.LINKEDIN, "apify/linkedin-profile-scraper");
        socialActors.put(SocialPlatform.TWITTER, "apify/twitter-scraper");
        socialActors.put(SocialPlatform.INSTAGRAM, "apify/instagram-scraper");
        socialActors.put(SocialPlatform.TIKTOK, "apify/tiktok-scraper");
        socialActors.put(SocialPlatform.YOUTUBE, "apify/youtube-scraper");

        webActors.put("default", "apify/web-scraper");
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Map<SocialPlatform, String> getSocialActors() {
        return socialActors;
    }

    public void setSocialActors(Map<SocialPlatform, String> socialActors) {
        this.socialActors = socialActors;
    }

    public Map<String, String> getWebActors() {
        return webActors;
    }

    public void setWebActors(Map<String, String> webActors) {
        this.webActors = webActors;
    }

    public String getActorId(SocialPlatform platform) {
        return socialActors.get(platform);
    }

    public String getWebActorId(String contentType) {
        return webActors.getOrDefault(contentType, webActors.get("default"));
    }
}
