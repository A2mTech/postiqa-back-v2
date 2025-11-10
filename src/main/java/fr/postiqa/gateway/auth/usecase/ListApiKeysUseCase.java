package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.ApiKeyEntity;
import fr.postiqa.database.repository.ApiKeyRepository;
import fr.postiqa.shared.dto.auth.ApiKeyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * List API Keys Use Case.
 * Single responsibility: List all API keys for a user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ListApiKeysUseCase {

    private final ApiKeyRepository apiKeyRepository;

    @Transactional(readOnly = true)
    public List<ApiKeyDto> execute(UUID userId) {
        log.debug("Listing API keys for user: {}", userId);

        List<ApiKeyEntity> apiKeys = apiKeyRepository.findByUserId(userId);

        return apiKeys.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Map ApiKeyEntity to ApiKeyDto (without plain-text key)
     */
    private ApiKeyDto mapToDto(ApiKeyEntity entity) {
        return ApiKeyDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .lastUsedAt(entity.getLastUsedAt())
            .expiresAt(entity.getExpiresAt())
            .active(entity.getActive())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
