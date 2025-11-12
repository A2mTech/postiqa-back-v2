package fr.postiqa.features.socialaccounts.adapter.in;

import fr.postiqa.features.socialaccounts.domain.model.ConnectionTestResult;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2AuthorizationUrl;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.usecase.*;
import fr.postiqa.features.socialaccounts.usecase.GenerateAuthorizationUrlUseCase.GenerateAuthUrlCommand;
import fr.postiqa.features.socialaccounts.usecase.ConnectSocialAccountUseCase.ConnectAccountCommand;
import fr.postiqa.features.socialaccounts.usecase.ListSocialAccountsUseCase.ListAccountsCommand;
import fr.postiqa.features.socialaccounts.usecase.GetSocialAccountUseCase.GetAccountCommand;
import fr.postiqa.features.socialaccounts.usecase.DisconnectSocialAccountUseCase.DisconnectCommand;
import fr.postiqa.features.socialaccounts.usecase.RefreshTokenUseCase.RefreshTokenCommand;
import fr.postiqa.features.socialaccounts.usecase.TestConnectionUseCase.TestConnectionCommand;
import fr.postiqa.shared.dto.socialaccount.*;
import fr.postiqa.shared.enums.SocialPlatform;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for social account operations (feature module).
 * This controller is internal to the feature module and should be called by business/agency controllers.
 */
@RestController
@RequestMapping("/api/internal/social-accounts")
@RequiredArgsConstructor
@Slf4j
public class SocialAccountController {

    private final GenerateAuthorizationUrlUseCase generateAuthorizationUrlUseCase;
    private final ConnectSocialAccountUseCase connectSocialAccountUseCase;
    private final DisconnectSocialAccountUseCase disconnectSocialAccountUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ListSocialAccountsUseCase listSocialAccountsUseCase;
    private final TestConnectionUseCase testConnectionUseCase;
    private final GetSocialAccountUseCase getSocialAccountUseCase;

    @GetMapping("/authorize/{platform}")
    public ResponseEntity<OAuth2AuthorizationUrlResponse> generateAuthorizationUrl(
        @PathVariable SocialPlatform platform,
        @RequestParam String redirectUri,
        @RequestParam(required = false, defaultValue = "openid,profile,email") String scopes
    ) {
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

    @PostMapping("/connect")
    public ResponseEntity<ConnectSocialAccountResponse> connectAccount(
        @Valid @RequestBody ConnectSocialAccountRequest request,
        @RequestParam UUID userId,
        @RequestParam UUID organizationId,
        @RequestParam(required = false) UUID clientId,
        @RequestParam String redirectUri,
        @RequestParam(required = false) String scopes
    ) {
        SocialAccount account = connectSocialAccountUseCase.execute(
            new ConnectAccountCommand(
                request.getPlatform(),
                request.getCode(),
                redirectUri,
                userId,
                organizationId,
                clientId,
                scopes
            )
        );

        SocialAccountDto accountDto = toDto(account);

        ConnectSocialAccountResponse response = ConnectSocialAccountResponse.builder()
            .account(accountDto)
            .success(true)
            .message("Successfully connected " + request.getPlatform() + " account")
            .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> disconnectAccount(
        @PathVariable UUID accountId,
        @RequestParam UUID organizationId
    ) {
        disconnectSocialAccountUseCase.execute(
            new DisconnectCommand(accountId, organizationId)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountId}/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@PathVariable UUID accountId) {
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

    @GetMapping
    public ResponseEntity<List<SocialAccountDto>> listAccounts(
        @RequestParam UUID organizationId,
        @RequestParam(required = false) UUID clientId,
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        List<SocialAccount> accounts = clientId != null
            ? listSocialAccountsUseCase.execute(new ListAccountsCommand(null, clientId, activeOnly))
            : listSocialAccountsUseCase.execute(new ListAccountsCommand(organizationId, null, activeOnly));

        List<SocialAccountDto> dtos = accounts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<SocialAccountDto> getAccount(
        @PathVariable UUID accountId,
        @RequestParam UUID organizationId,
        @RequestParam(required = false) UUID clientId
    ) {
        SocialAccount account = clientId != null
            ? getSocialAccountUseCase.execute(new GetAccountCommand(accountId, null, clientId))
            : getSocialAccountUseCase.execute(new GetAccountCommand(accountId, organizationId, null));

        return ResponseEntity.ok(toDto(account));
    }

    @PostMapping("/{accountId}/test")
    public ResponseEntity<TestConnectionResponse> testConnection(
        @PathVariable UUID accountId,
        @RequestParam UUID organizationId
    ) {
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
