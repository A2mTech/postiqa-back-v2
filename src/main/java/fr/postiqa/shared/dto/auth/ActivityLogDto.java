package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for ActivityLog entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto {

    private UUID id;
    private UserDto user;
    private OrganizationDto organization;
    private ClientDto client;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private Instant timestamp;
}
