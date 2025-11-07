package fr.postiqa.core.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response from Apify when starting a new run
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApifyRunResponse(
    @JsonProperty("id") String id,
    @JsonProperty("actId") String actId,
    @JsonProperty("status") String status,
    @JsonProperty("startedAt") LocalDateTime startedAt,
    @JsonProperty("finishedAt") LocalDateTime finishedAt,
    @JsonProperty("defaultDatasetId") String defaultDatasetId,
    @JsonProperty("defaultKeyValueStoreId") String defaultKeyValueStoreId
) {}
