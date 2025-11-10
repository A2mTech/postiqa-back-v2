package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Client entity (agency multi-tenant).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {

    private UUID id;
    private UUID agencyId;
    private String clientName;
    private String clientEmail;
    private Map<String, Object> clientInfo;
    private Boolean active;
    private Instant createdAt;
}
