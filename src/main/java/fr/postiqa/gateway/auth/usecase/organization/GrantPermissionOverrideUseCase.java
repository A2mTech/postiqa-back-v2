package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationEntity;
import fr.postiqa.database.entity.PermissionEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.entity.UserPermissionOverrideEntity;
import fr.postiqa.database.repository.OrganizationRepository;
import fr.postiqa.database.repository.PermissionRepository;
import fr.postiqa.database.repository.UserPermissionOverrideRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.shared.dto.auth.GrantPermissionRequest;
import fr.postiqa.shared.exception.auth.CannotModifySelfException;
import fr.postiqa.shared.exception.auth.OrganizationNotFoundException;
import fr.postiqa.shared.exception.auth.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for granting or revoking custom permissions to a user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GrantPermissionOverrideUseCase {

    private final UserPermissionOverrideRepository permissionOverrideRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionRepository permissionRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public void execute(GrantPermissionRequest request, UUID grantedByUserId) {
        // Prevent self-modification
        if (request.getUserId().equals(grantedByUserId)) {
            throw new CannotModifySelfException("Cannot modify your own permissions");
        }

        // Validate entities exist
        UserEntity user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        OrganizationEntity organization = organizationRepository.findById(request.getOrganizationId())
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        PermissionEntity permission = permissionRepository.findById(request.getPermissionId())
            .orElseThrow(() -> new RuntimeException("Permission not found"));

        UserEntity grantedBy = userRepository.findById(grantedByUserId)
            .orElseThrow(() -> new UserNotFoundException("Granting user not found"));

        // Check if override already exists
        Optional<UserPermissionOverrideEntity> existingOverride =
            permissionOverrideRepository.findByUserIdAndOrganizationIdAndPermissionId(
                request.getUserId(),
                request.getOrganizationId(),
                request.getPermissionId()
            );

        UserPermissionOverrideEntity override;

        if (existingOverride.isPresent()) {
            // Update existing override
            override = existingOverride.get();
            boolean oldGranted = override.getGranted();
            override.setGranted(request.getGranted());
            override.setReason(request.getReason());
            override.setGrantedBy(grantedBy);

            log.info("Updated permission override for {} in organization {}: {} {} -> {}",
                user.getEmail(), organization.getName(), permission.getPermissionName(),
                oldGranted, request.getGranted());
        } else {
            // Create new override
            override = UserPermissionOverrideEntity.builder()
                .user(user)
                .organization(organization)
                .permission(permission)
                .granted(request.getGranted())
                .reason(request.getReason())
                .grantedBy(grantedBy)
                .build();

            log.info("Created permission override for {} in organization {}: {} = {}",
                user.getEmail(), organization.getName(), permission.getPermissionName(),
                request.getGranted());
        }

        permissionOverrideRepository.save(override);

        // Log activity
        activityLogService.logActivity(
            grantedByUserId,
            request.getOrganizationId(),
            null,
            request.getGranted() ? "PERMISSION_GRANTED" : "PERMISSION_REVOKED",
            "PERMISSION_OVERRIDE",
            override.getId(),
            null,
            null,
            Map.of(
                "targetUserEmail", user.getEmail(),
                "targetUserId", user.getId().toString(),
                "permission", permission.getPermissionName(),
                "granted", request.getGranted(),
                "reason", request.getReason() != null ? request.getReason() : ""
            )
        );
    }
}
