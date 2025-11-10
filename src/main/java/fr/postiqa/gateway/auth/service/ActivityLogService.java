package fr.postiqa.gateway.auth.service;

import fr.postiqa.database.entity.ActivityLogEntity;
import fr.postiqa.database.entity.ClientEntity;
import fr.postiqa.database.entity.OrganizationEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.ActivityLogRepository;
import fr.postiqa.database.repository.ClientRepository;
import fr.postiqa.database.repository.OrganizationRepository;
import fr.postiqa.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing activity logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;

    /**
     * Log an activity.
     */
    @Transactional
    public ActivityLogEntity logActivity(
        UUID userId,
        UUID organizationId,
        UUID clientId,
        String action,
        String resourceType,
        UUID resourceId,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata
    ) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        OrganizationEntity organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new RuntimeException("Organization not found"));

        ClientEntity client = null;
        if (clientId != null) {
            client = clientRepository.findById(clientId).orElse(null);
        }

        ActivityLogEntity activityLog = ActivityLogEntity.builder()
            .user(user)
            .organization(organization)
            .client(client)
            .action(action)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .metadata(metadata)
            .build();

        return activityLogRepository.save(activityLog);
    }

    /**
     * Get activity logs for an organization.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogEntity> getOrganizationActivities(UUID organizationId, Pageable pageable) {
        return activityLogRepository.findByOrganizationId(organizationId, pageable);
    }

    /**
     * Get activity logs for a user in an organization.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogEntity> getUserActivities(UUID userId, UUID organizationId, Pageable pageable) {
        return activityLogRepository.findByUserIdAndOrganizationId(userId, organizationId, pageable);
    }

    /**
     * Get activity logs in a time range.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogEntity> getActivitiesByTimeRange(
        UUID organizationId,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    ) {
        return activityLogRepository.findByOrganizationIdAndTimestampBetween(
            organizationId,
            startTime,
            endTime,
            pageable
        );
    }

    /**
     * Cleanup old activity logs.
     */
    @Transactional
    public void cleanupOldLogs(Instant olderThan) {
        activityLogRepository.deleteByTimestampBefore(olderThan);
        log.info("Cleaned up activity logs older than {}", olderThan);
    }
}
