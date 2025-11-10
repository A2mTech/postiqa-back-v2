package fr.postiqa.business.controller;

import fr.postiqa.features.socialaccounts.usecase.*;
import fr.postiqa.features.socialaccounts.domain.model.ConnectionTestResult;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.shared.dto.socialaccount.*;
import fr.postiqa.shared.enums.SocialPlatform;
import fr.postiqa.shared.exception.auth.InsufficientPermissionsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business API controller for social account management.
 * Endpoints for businesses managing their own social media accounts.
 */
@RestController
@RequestMapping("/api/business/social-accounts")
@RequiredArgsConstructor
@Slf4j
public class BusinessSocialAccountController {

    private final GenerateAuthorizationUrlUseCase generateAuthorizationUrlUseCase;
    private final ConnectSocialAccountUseCase connectSocialAccountUseCase;
    private final DisconnectSocialAccountUseCase disconnectSocialAccountUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ListSocialAccountsUseCase listSocialAccountsUseCase;
    private final TestConnectionUseCase testConnectionUseCase;
    private final GetSocialAccountUseCase getSocialAccountUseCase;

    /**
     * Generate OAuth2 authorization URL to connect a social account.
     */
    @GetMapping("/authorize/{platform}")
    public ResponseEntity<OAuth2AuthorizationUrlResponse> authorizeAccount(
        @PathVariable SocialPlatform platform,
        @RequestParam String redirectUri,
        @RequestParam(required = false, defaultValue = "openid,profile,email") String scopes
    ) {
        log.info("Generating authorization URL for platform: {}", platform);

        String[] scopeArray = scopes.split(",");

        OAuth2AuthorizationUrl authUrl = generateAuthorizationUrlUseCase.execute(
            platform,
            redirectUri,
            scopeArray
        );

        OAuth2AuthorizationUrlResponse response = OAuth2AuthorizationUrlResponse.builder()
            .platform(platform)
            .authorizationUrl(authUrl.getUrl())
            .state(authUrl.getState())
            .message("Please visit the authorization URL to connect your " + platform + " account")
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Complete OAuth2 callback and connect social account.
     */
    @PostMapping("/callback")
    public ResponseEntity<ConnectSocialAccountResponse> connectAccount(
        @Valid @RequestBody ConnectSocialAccountRequest request,
        @RequestParam String redirectUri,
        @RequestParam(required = false) String scopes,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Connecting social account for platform: {}", request.getPlatform());

        UUID userId = userDetails.getId();
        UUID organizationId = extractOrganizationId(userDetails);

        SocialAccount account = connectSocialAccountUseCase.execute(
            request.getPlatform(),
            request.getCode(),
            redirectUri,
            userId,
            organizationId,
            null, // No client for business
            scopes
        );

        SocialAccountDto accountDto = toDto(account);

        ConnectSocialAccountResponse response = ConnectSocialAccountResponse.builder()
            .account(accountDto)
            .success(true)
            .message("Successfully connected " + request.getPlatform() + " account")
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * List all connected social accounts for the business.
     */
    @GetMapping
    public ResponseEntity<List<SocialAccountDto>> listAccounts(
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID organizationId = extractOrganizationId(userDetails);

        List<SocialAccount> accounts = listSocialAccountsUseCase.executeForOrganization(organizationId, activeOnly);

        List<SocialAccountDto> dtos = accounts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific social account by ID.
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<SocialAccountDto> getAccount(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID organizationId = extractOrganizationId(userDetails);

        SocialAccount account = getSocialAccountUseCase.execute(accountId, organizationId);

        return ResponseEntity.ok(toDto(account));
    }

    /**
     * Disconnect a social account.
     */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> disconnectAccount(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Disconnecting social account: {}", accountId);

        UUID organizationId = extractOrganizationId(userDetails);

        disconnectSocialAccountUseCase.execute(accountId, organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh access token for a social account.
     */
    @PostMapping("/{accountId}/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@PathVariable UUID accountId) {
        log.info("Refreshing token for account: {}", accountId);

        SocialAccount account = refreshTokenUseCase.execute(accountId);

        RefreshTokenResponse response = RefreshTokenResponse.builder()
            .accountId(account.getId())
            .success(true)
            .newTokenExpiresAt(account.getToken().getExpiresAt())
            .message("Token refreshed successfully")
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Test if a social account connection is valid.
     */
    @PostMapping("/{accountId}/test")
    public ResponseEntity<TestConnectionResponse> testConnection(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Testing connection for account: {}", accountId);

        UUID organizationId = extractOrganizationId(userDetails);

        ConnectionTestResult result = testConnectionUseCase.execute(accountId, organizationId);

        TestConnectionResponse response = TestConnectionResponse.builder()
            .accountId(accountId)
            .isValid(result.getIsValid())
            .message(result.getMessage())
            .errorDetails(result.getErrorDetails())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Extract organization ID from authenticated user's scopes.
     * Business users have exactly one organization scope.
     */
    private UUID extractOrganizationId(CustomUserDetails userDetails) {
        if (userDetails.getScopes() == null || userDetails.getScopes().isEmpty()) {
            throw new InsufficientPermissionsException("User has no organization scope");
        }
        UUID organizationId = userDetails.getScopes().get(0).getOrganizationId();
        if (organizationId == null) {
            throw new InsufficientPermissionsException("User's scope has no organization ID");
        }
        return organizationId;
    }

    private SocialAccountDto toDto(SocialAccount account) {
        boolean tokenExpired = account.isTokenExpired();
        boolean tokenExpiringSoon = account.isTokenExpiringSoon();

        return SocialAccountDto.builder()
            .id(account.getId())
            .userId(account.getUserId())
            .organizationId(account.getOrganizationId())
            .clientId(account.getClientId())
            .platform(account.getPlatform())
            .platformAccountId(account.getPlatformAccountId())
            .accountName(account.getAccountName())
            .accountHandle(account.getAccountHandle())
            .accountAvatarUrl(account.getAccountAvatarUrl())
            .tokenExpiresAt(account.getToken() != null ? account.getToken().getExpiresAt() : null)
            .scopes(account.getScopes())
            .platformMetadata(account.getPlatformMetadata())
            .active(account.getActive())
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .tokenExpired(tokenExpired)
            .tokenExpiringSoon(tokenExpiringSoon)
            .build();
    }
}
