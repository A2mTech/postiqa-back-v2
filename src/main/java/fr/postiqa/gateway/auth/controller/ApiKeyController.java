package fr.postiqa.gateway.auth.controller;

import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.gateway.auth.usecase.CreateApiKeyUseCase;
import fr.postiqa.gateway.auth.usecase.ListApiKeysUseCase;
import fr.postiqa.gateway.auth.usecase.RevokeApiKeyUseCase;
import fr.postiqa.shared.dto.auth.ApiKeyDto;
import fr.postiqa.shared.dto.auth.ApiKeyRequest;
import fr.postiqa.shared.dto.auth.ApiKeyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API Key Controller.
 * Manages API key operations for authenticated users.
 */
@RestController
@RequestMapping("/api/auth/api-keys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final CreateApiKeyUseCase createApiKeyUseCase;
    private final ListApiKeysUseCase listApiKeysUseCase;
    private final RevokeApiKeyUseCase revokeApiKeyUseCase;

    /**
     * Create a new API key
     * POST /api/auth/api-keys
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ApiKeyRequest request
    ) {
        log.info("Creating API key for user: {}", userDetails.getEmail());
        ApiKeyResponse response = createApiKeyUseCase.execute(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all API keys for current user
     * GET /api/auth/api-keys
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> listApiKeys(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("Listing API keys for user: {}", userDetails.getEmail());
        List<ApiKeyDto> apiKeys = listApiKeysUseCase.execute(userDetails.getId());
        return ResponseEntity.ok(apiKeys);
    }

    /**
     * Revoke (deactivate) an API key
     * DELETE /api/auth/api-keys/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID id
    ) {
        log.info("Revoking API key {} for user: {}", id, userDetails.getEmail());
        revokeApiKeyUseCase.execute(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
