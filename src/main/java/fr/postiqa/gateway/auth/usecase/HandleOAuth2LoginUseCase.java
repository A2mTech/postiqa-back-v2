package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.OAuthConnectionEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.OAuthConnectionRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.jwt.JwtTokenProvider;
import fr.postiqa.shared.dto.auth.LoginResponse;
import fr.postiqa.shared.dto.auth.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handle OAuth2 Login Use Case.
 * Single responsibility: Process OAuth2 login, create/link user, generate JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HandleOAuth2LoginUseCase {

    private final UserRepository userRepository;
    private final OAuthConnectionRepository oauthConnectionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StoreRefreshTokenUseCase storeRefreshTokenUseCase;

    @Transactional
    public LoginResponse execute(
        OAuth2User oauth2User,
        String provider,
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt
    ) {
        log.debug("Processing OAuth2 login for provider: {}", provider);

        // Extract OAuth2 user info
        String providerUserId = oauth2User.getName(); // OAuth2 provider's user ID
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String firstName = extractFirstName(name, oauth2User);
        String lastName = extractLastName(name, oauth2User);

        // Find or create user
        UserEntity user = findOrCreateUser(email, firstName, lastName);

        // Create or update OAuth connection
        createOrUpdateOAuthConnection(
            user, provider, providerUserId, email, name,
            oauth2User.getAttributes(), accessToken, refreshToken, tokenExpiresAt
        );

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate JWT tokens
        Map<String, Object> claims = buildClaims(user);
        List<String> authorities = extractAuthorities(user);

        String jwtAccessToken = jwtTokenProvider.generateAccessToken(
            user.getEmail(),
            authorities,
            claims
        );
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Store refresh token
        storeRefreshTokenUseCase.execute(user.getId(), jwtRefreshToken);

        // Build response
        return buildLoginResponse(jwtAccessToken, jwtRefreshToken, user, authorities);
    }

    /**
     * Find existing user by email or create new user
     */
    private UserEntity findOrCreateUser(String email, String firstName, String lastName) {
        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(email);

        if (existingUser.isPresent()) {
            log.debug("Found existing user: {}", email);
            return existingUser.get();
        }

        // Create new user from OAuth2 data
        log.info("Creating new user from OAuth2: {}", email);
        UserEntity newUser = UserEntity.builder()
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .emailVerified(true) // OAuth2 email is pre-verified
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .build();

        return userRepository.save(newUser);
    }

    /**
     * Create or update OAuth connection
     */
    private void createOrUpdateOAuthConnection(
        UserEntity user,
        String provider,
        String providerUserId,
        String providerEmail,
        String providerName,
        Map<String, Object> providerData,
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt
    ) {
        Optional<OAuthConnectionEntity> existingConnection =
            oauthConnectionRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (existingConnection.isPresent()) {
            // Update existing connection
            OAuthConnectionEntity connection = existingConnection.get();
            connection.setProviderEmail(providerEmail);
            connection.setProviderName(providerName);
            connection.setProviderData(providerData);
            connection.setAccessToken(accessToken);
            connection.setRefreshToken(refreshToken);
            connection.setTokenExpiresAt(tokenExpiresAt);
            oauthConnectionRepository.save(connection);
            log.debug("Updated OAuth connection for provider: {}", provider);
        } else {
            // Create new connection
            OAuthConnectionEntity newConnection = OAuthConnectionEntity.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .providerName(providerName)
                .providerData(providerData)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .build();
            oauthConnectionRepository.save(newConnection);
            log.info("Created new OAuth connection for provider: {}", provider);
        }
    }

    /**
     * Extract first name from OAuth2 data
     */
    private String extractFirstName(String name, OAuth2User oauth2User) {
        // Try given_name attribute first
        String givenName = oauth2User.getAttribute("given_name");
        if (givenName != null) {
            return givenName;
        }

        // Try first_name attribute
        String firstName = oauth2User.getAttribute("first_name");
        if (firstName != null) {
            return firstName;
        }

        // Fall back to splitting name
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }

        return name;
    }

    /**
     * Extract last name from OAuth2 data
     */
    private String extractLastName(String name, OAuth2User oauth2User) {
        // Try family_name attribute first
        String familyName = oauth2User.getAttribute("family_name");
        if (familyName != null) {
            return familyName;
        }

        // Try last_name attribute
        String lastName = oauth2User.getAttribute("last_name");
        if (lastName != null) {
            return lastName;
        }

        // Fall back to splitting name
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ");
            return parts.length > 1 ? parts[parts.length - 1] : null;
        }

        return null;
    }

    /**
     * Build JWT claims
     */
    private Map<String, Object> buildClaims(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId().toString());
        // Note: organization_id and client_id would be added here if user has roles with scopes
        return claims;
    }

    /**
     * Extract authorities from user (roles + permissions)
     */
    private List<String> extractAuthorities(UserEntity user) {
        Set<String> authorities = new HashSet<>();

        // Add roles with ROLE_ prefix
        user.getUserRoles().forEach(userRole -> {
            authorities.add("ROLE_" + userRole.getRole().getName());

            // Add permissions
            userRole.getRole().getRolePermissions().forEach(rolePermission ->
                authorities.add(rolePermission.getPermission().getPermissionName())
            );
        });

        return new ArrayList<>(authorities);
    }

    /**
     * Build login response
     */
    private LoginResponse buildLoginResponse(
        String accessToken,
        String refreshToken,
        UserEntity user,
        List<String> authorities
    ) {
        List<String> roles = authorities.stream()
            .filter(auth -> auth.startsWith("ROLE_"))
            .map(auth -> auth.substring(5))
            .collect(Collectors.toList());

        List<String> permissions = authorities.stream()
            .filter(auth -> !auth.startsWith("ROLE_"))
            .collect(Collectors.toList());

        UserDto userDto = UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .emailVerified(user.getEmailVerified())
            .enabled(user.getEnabled())
            .accountLocked(user.getAccountLocked())
            .lastLoginAt(user.getLastLoginAt())
            .build();

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
            .user(userDto)
            .roles(roles)
            .permissions(permissions)
            .build();
    }
}
