package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.ApiKeyEntity;
import fr.postiqa.database.repository.ApiKeyRepository;
import fr.postiqa.shared.exception.auth.ApiKeyInvalidException;
import fr.postiqa.shared.exception.auth.InsufficientPermissionsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Revoke API Key Use Case.
 * Single responsibility: Revoke (deactivate) an API key.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RevokeApiKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public void execute(UUID userId, UUID apiKeyId) {
        log.debug("Revoking API key: {} for user: {}", apiKeyId, userId);

        // Find API key
        ApiKeyEntity apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new ApiKeyInvalidException("API key not found"));

        // Verify ownership
        if (!apiKey.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to revoke API key {} owned by user {}",
                userId, apiKeyId, apiKey.getUser().getId());
            throw new InsufficientPermissionsException("You can only revoke your own API keys");
        }

        // Revoke (deactivate)
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        log.info("API key revoked successfully: {} for user: {}", apiKeyId, userId);
    }
}
