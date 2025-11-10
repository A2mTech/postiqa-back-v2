package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for filtering activity logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogFilterRequest {

    private UUID userId;
    private UUID organizationId;
    private UUID clientId;
    private String action;
    private String resourceType;
    private Instant startTime;
    private Instant endTime;
    private Integer page;
    private Integer size;
}
