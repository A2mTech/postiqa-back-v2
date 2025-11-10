package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.ApiKeyEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.ApiKeyRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.shared.dto.auth.ApiKeyRequest;
import fr.postiqa.shared.dto.auth.ApiKeyResponse;
import fr.postiqa.shared.exception.auth.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Create API Key Use Case.
 * Single responsibility: Generate and store a new API key for a user.
 * IMPORTANT: Returns plain-text key only once during creation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateApiKeyUseCase {

    private static final int API_KEY_BYTES = 32; // 256 bits
    private static final String HASH_ALGORITHM = "SHA-256";

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApiKeyResponse execute(UUID userId, ApiKeyRequest request) {
        log.debug("Creating API key for user: {}", userId);

        // Load user
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Generate random API key
        String plainTextKey = generateApiKey();

        // Hash the API key
        String keyHash = hashApiKey(plainTextKey);

        // Calculate expiration
        Instant expiresAt = calculateExpiration(request.getExpiresInDays());

        // Create entity
        ApiKeyEntity apiKey = ApiKeyEntity.builder()
            .user(user)
            .keyHash(keyHash)
            .name(request.getName())
            .description(request.getDescription())
            .expiresAt(expiresAt)
            .active(true)
            .build();

        apiKeyRepository.save(apiKey);

        log.info("API key created successfully for user: {} with name: {}", userId, request.getName());

        // Return response with plain-text key (only returned once)
        return ApiKeyResponse.builder()
            .id(apiKey.getId())
            .name(apiKey.getName())
            .description(apiKey.getDescription())
            .key(plainTextKey) // IMPORTANT: Plain-text key only returned once
            .expiresAt(apiKey.getExpiresAt())
            .createdAt(apiKey.getCreatedAt())
            .build();
    }

    /**
     * Generate a secure random API key
     */
    private String generateApiKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[API_KEY_BYTES];
        secureRandom.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
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

    /**
     * Calculate expiration date
     */
    private Instant calculateExpiration(Integer expiresInDays) {
        if (expiresInDays == null || expiresInDays <= 0) {
            return null; // No expiration
        }
        return Instant.now().plusSeconds(expiresInDays * 24L * 60L * 60L);
    }
}
