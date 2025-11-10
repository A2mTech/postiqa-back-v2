package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.ActivityLogEntity;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.shared.dto.auth.ActivityLogDto;
import fr.postiqa.shared.dto.auth.ActivityLogFilterRequest;
import fr.postiqa.shared.dto.auth.ClientDto;
import fr.postiqa.shared.dto.auth.OrganizationDto;
import fr.postiqa.shared.dto.auth.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for getting activity logs with filters.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetMemberActivityLogsUseCase {

    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public Page<ActivityLogDto> execute(ActivityLogFilterRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage() != null ? request.getPage() : 0,
            request.getSize() != null ? request.getSize() : 20,
            Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<ActivityLogEntity> logs;

        if (request.getUserId() != null && request.getOrganizationId() != null) {
            logs = activityLogService.getUserActivities(
                request.getUserId(),
                request.getOrganizationId(),
                pageable
            );
        } else if (request.getStartTime() != null && request.getEndTime() != null && request.getOrganizationId() != null) {
            logs = activityLogService.getActivitiesByTimeRange(
                request.getOrganizationId(),
                request.getStartTime(),
                request.getEndTime(),
                pageable
            );
        } else if (request.getOrganizationId() != null) {
            logs = activityLogService.getOrganizationActivities(
                request.getOrganizationId(),
                pageable
            );
        } else {
            throw new IllegalArgumentException("OrganizationId is required");
        }

        return logs.map(this::toDto);
    }

    private ActivityLogDto toDto(ActivityLogEntity log) {
        return ActivityLogDto.builder()
            .id(log.getId())
            .user(UserDto.builder()
                .id(log.getUser().getId())
                .email(log.getUser().getEmail())
                .firstName(log.getUser().getFirstName())
                .lastName(log.getUser().getLastName())
                .build())
            .organization(OrganizationDto.builder()
                .id(log.getOrganization().getId())
                .name(log.getOrganization().getName())
                .type(log.getOrganization().getType().name())
                .build())
            .client(log.getClient() != null ? ClientDto.builder()
                .id(log.getClient().getId())
                .clientName(log.getClient().getClientName())
                .build() : null)
            .action(log.getAction())
            .resourceType(log.getResourceType())
            .resourceId(log.getResourceId())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .metadata(log.getMetadata())
            .timestamp(log.getTimestamp())
            .build();
    }
}
