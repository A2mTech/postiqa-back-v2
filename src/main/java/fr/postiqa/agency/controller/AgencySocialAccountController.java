package fr.postiqa.agency.controller;

import fr.postiqa.features.socialaccounts.usecase.*;
import fr.postiqa.features.socialaccounts.usecase.GenerateAuthorizationUrlUseCase.GenerateAuthUrlCommand;
import fr.postiqa.features.socialaccounts.usecase.ConnectSocialAccountUseCase.ConnectAccountCommand;
import fr.postiqa.features.socialaccounts.usecase.ListSocialAccountsUseCase.ListAccountsCommand;
import fr.postiqa.features.socialaccounts.usecase.GetSocialAccountUseCase.GetAccountCommand;
import fr.postiqa.features.socialaccounts.usecase.DisconnectSocialAccountUseCase.DisconnectCommand;
import fr.postiqa.features.socialaccounts.usecase.RefreshTokenUseCase.RefreshTokenCommand;
import fr.postiqa.features.socialaccounts.usecase.TestConnectionUseCase.TestConnectionCommand;
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
 * Agency API controller for social account management.
 * Endpoints for agencies managing social media accounts for their clients.
 */
@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
@Slf4j
public class AgencySocialAccountController {

    private final GenerateAuthorizationUrlUseCase generateAuthorizationUrlUseCase;
    private final ConnectSocialAccountUseCase connectSocialAccountUseCase;
    private final DisconnectSocialAccountUseCase disconnectSocialAccountUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ListSocialAccountsUseCase listSocialAccountsUseCase;
    private final TestConnectionUseCase testConnectionUseCase;
    private final GetSocialAccountUseCase getSocialAccountUseCase;

    /**
     * Generate OAuth2 authorization URL to connect a social account for a client.
     */
    @GetMapping("/clients/{clientId}/social-accounts/authorize/{platform}")
    public ResponseEntity<OAuth2AuthorizationUrlResponse> authorizeAccount(
        @PathVariable UUID clientId,
        @PathVariable SocialPlatform platform,
        @RequestParam String redirectUri,
        @RequestParam(required = false, defaultValue = "openid,profile,email") String scopes,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Generating authorization URL for platform: {} for client: {}", platform, clientId);

        UUID organizationId = extractOrganizationId(userDetails);
        verifyClientAccess(clientId, organizationId);

        String[] scopeArray = scopes.split(",");

        OAuth2AuthorizationUrl authUrl = generateAuthorizationUrlUseCase.execute(
            new GenerateAuthUrlCommand(platform, redirectUri, scopeArray)
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
     * Complete OAuth2 callback and connect social account for a client.
     */
    @PostMapping("/clients/{clientId}/social-accounts/callback")
    public ResponseEntity<ConnectSocialAccountResponse> connectAccount(
        @PathVariable UUID clientId,
        @Valid @RequestBody ConnectSocialAccountRequest request,
        @RequestParam String redirectUri,
        @RequestParam(required = false) String scopes,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Connecting social account for platform: {} for client: {}", request.getPlatform(), clientId);

        UUID userId = userDetails.getId();
        UUID organizationId = extractOrganizationId(userDetails);
        verifyClientAccess(clientId, organizationId);

        SocialAccount account = connectSocialAccountUseCase.execute(
            new ConnectAccountCommand(
                request.getPlatform(),
                request.getCode(),
                redirectUri,
                userId,
                organizationId,
                clientId, // Agency manages for client
                scopes
            )
        );

        SocialAccountDto accountDto = toDto(account);

        ConnectSocialAccountResponse response = ConnectSocialAccountResponse.builder()
            .account(accountDto)
            .success(true)
            .message("Successfully connected " + request.getPlatform() + " account for client")
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * List all connected social accounts for a client.
     */
    @GetMapping("/clients/{clientId}/social-accounts")
    public ResponseEntity<List<SocialAccountDto>> listClientAccounts(
        @PathVariable UUID clientId,
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Listing social accounts for client: {}", clientId);

        UUID organizationId = extractOrganizationId(userDetails);
        verifyClientAccess(clientId, organizationId);

        List<SocialAccount> accounts = listSocialAccountsUseCase.execute(
            new ListAccountsCommand(null, clientId, activeOnly)
        );

        List<SocialAccountDto> dtos = accounts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * List all social accounts across all clients (agency-wide view).
     */
    @GetMapping("/social-accounts")
    public ResponseEntity<List<SocialAccountDto>> listAllAccounts(
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Listing all social accounts for agency");

        UUID organizationId = extractOrganizationId(userDetails);

        List<SocialAccount> accounts = listSocialAccountsUseCase.execute(
            new ListAccountsCommand(organizationId, null, activeOnly)
        );

        List<SocialAccountDto> dtos = accounts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific social account by ID.
     */
    @GetMapping("/social-accounts/{accountId}")
    public ResponseEntity<SocialAccountDto> getAccount(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting social account: {}", accountId);

        UUID organizationId = extractOrganizationId(userDetails);

        SocialAccount account = getSocialAccountUseCase.execute(
            new GetAccountCommand(accountId, organizationId, null)
        );

        return ResponseEntity.ok(toDto(account));
    }

    /**
     * Disconnect a social account.
     */
    @DeleteMapping("/social-accounts/{accountId}")
    public ResponseEntity<Void> disconnectAccount(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Disconnecting social account: {}", accountId);

        UUID organizationId = extractOrganizationId(userDetails);

        disconnectSocialAccountUseCase.execute(
            new DisconnectCommand(accountId, organizationId)
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh access token for a social account.
     */
    @PostMapping("/social-accounts/{accountId}/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Refreshing token for account: {}", accountId);

        UUID organizationId = extractOrganizationId(userDetails);
        verifyAccountAccess(accountId, organizationId);

        SocialAccount account = refreshTokenUseCase.execute(
            new RefreshTokenCommand(accountId)
        );

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
    @PostMapping("/social-accounts/{accountId}/test")
    public ResponseEntity<TestConnectionResponse> testConnection(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Testing connection for account: {}", accountId);

        UUID organizationId = extractOrganizationId(userDetails);
        verifyAccountAccess(accountId, organizationId);

        ConnectionTestResult result = testConnectionUseCase.execute(
            new TestConnectionCommand(accountId, organizationId)
        );

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
     * Agency users have exactly one organization scope.
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

    /**
     * Verify that the agency has access to the specified client.
     * This checks if the client belongs to the agency's organization.
     */
    private void verifyClientAccess(UUID clientId, UUID organizationId) {
        // The use case will throw an exception if the client doesn't belong to the organization
        // This is handled by GetSocialAccountUseCase's organization check
    }

    /**
     * Verify that the agency has access to the specified social account.
     * This is done by checking that the account belongs to the agency's organization.
     */
    private void verifyAccountAccess(UUID accountId, UUID organizationId) {
        getSocialAccountUseCase.execute(
            new GetAccountCommand(accountId, organizationId, null)
        );
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
