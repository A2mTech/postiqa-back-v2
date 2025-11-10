package fr.postiqa.features.postmanagement.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Configuration for R2 storage using AWS S3 SDK.
 * Cloudflare R2 is S3-compatible.
 */
@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2StorageConfiguration {

    @Bean
    public S3Client r2Client(R2Properties r2Properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            r2Properties.accessKey(),
            r2Properties.secretKey()
        );

        return S3Client.builder()
            .endpointOverride(URI.create(r2Properties.endpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(r2Properties.region()))
            .build();
    }
}
