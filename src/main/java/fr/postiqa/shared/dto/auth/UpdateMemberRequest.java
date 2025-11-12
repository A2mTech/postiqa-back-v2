package fr.postiqa.shared.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating a member's information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {

    private UUID memberId;
    private UUID managerId;
    private String position;
    private String title;
    private String status;  // ACTIVE, SUSPENDED
}
