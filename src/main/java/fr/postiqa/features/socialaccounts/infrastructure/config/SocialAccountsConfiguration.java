package fr.postiqa.features.socialaccounts.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for social accounts feature.
 * Provides beans for HTTP clients and other infrastructure components.
 */
@Configuration
public class SocialAccountsConfiguration {

    /**
     * RestTemplate for OAuth2 HTTP requests.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
