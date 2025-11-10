package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing organization hierarchy tree structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationHierarchyDto {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String position;
    private String title;
    private List<String> roles;
    private Integer directReportsCount;
    private List<OrganizationHierarchyDto> directReports;
}
