package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for Permission entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {

    private UUID id;
    private String resource;
    private String action;
    private String description;

    /**
     * Returns permission name in format RESOURCE:ACTION
     */
    public String getPermissionName() {
        return resource + ":" + action;
    }
}
