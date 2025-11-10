package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.ApiKeyEntity;
import fr.postiqa.database.repository.ApiKeyRepository;
import fr.postiqa.shared.exception.auth.ApiKeyInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Validate API Key Use Case.
 * Single responsibility: Validate API key and return associated user info.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateApiKeyUseCase {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public ApiKeyEntity execute(String plainTextKey) {
        log.debug("Validating API key");

        // Hash the provided key
        String keyHash = hashApiKey(plainTextKey);

        // Find API key by hash
        ApiKeyEntity apiKey = apiKeyRepository.findByKeyHashAndActiveTrue(keyHash)
            .orElseThrow(() -> new ApiKeyInvalidException("Invalid or inactive API key"));

        // Check expiration
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            log.warn("API key expired: {}", apiKey.getId());
            throw new ApiKeyInvalidException("API key has expired");
        }

        // Update last used timestamp
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        log.debug("API key validated successfully for user: {}", apiKey.getUser().getId());
        return apiKey;
    }

    /**
     * Hash API key using SHA-256
     */
    private String hashApiKey(String plainTextKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(plainTextKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash API key: {}", e.getMessage());
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
