package fr.postiqa.core.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response from Apify dataset items endpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApifyDatasetResponse(
    @JsonProperty("count") Integer count,
    @JsonProperty("offset") Integer offset,
    @JsonProperty("limit") Integer limit,
    @JsonProperty("total") Integer total,
    @JsonProperty("items") List<Map<String, Object>> items
) {}
