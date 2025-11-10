package fr.postiqa.gateway.auth.service;

import fr.postiqa.database.entity.RefreshTokenEntity;
import fr.postiqa.database.entity.RoleEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.entity.UserRoleEntity;
import fr.postiqa.database.repository.RefreshTokenRepository;
import fr.postiqa.database.repository.RoleRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.gateway.auth.jwt.JwtTokenProvider;
import fr.postiqa.shared.dto.auth.*;
import fr.postiqa.shared.exception.auth.InvalidCredentialsException;
import fr.postiqa.shared.exception.auth.TokenExpiredException;
import fr.postiqa.shared.exception.auth.InvalidTokenException;
import fr.postiqa.shared.exception.auth.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authentication Service.
 * Handles user login, registration, token refresh, and logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Login user and return JWT tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("user_id", userDetails.getId().toString());

            // Add organization and client scopes if available
            if (!userDetails.getScopes().isEmpty()) {
                CustomUserDetails.ScopeInfo primaryScope = userDetails.getScopes().get(0);
                if (primaryScope.getOrganizationId() != null) {
                    claims.put("organization_id", primaryScope.getOrganizationId().toString());
                }
                if (primaryScope.getClientId() != null) {
                    claims.put("client_id", primaryScope.getClientId().toString());
                }
            }

            List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

            String accessToken = jwtTokenProvider.generateAccessToken(
                userDetails.getUsername(),
                authorities,
                claims
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUsername());

            // Store refresh token
            storeRefreshToken(userDetails.getId(), refreshToken);

            // Update last login
            UserEntity user = userRepository.findById(userDetails.getId()).orElseThrow();
            user.setLastLoginAt(Instant.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            // Extract roles and permissions
            List<String> roles = authorities.stream()
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .collect(Collectors.toList());

            List<String> permissions = authorities.stream()
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toList());

            UserDto userDto = UserDto.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .emailVerified(userDetails.isEmailVerified())
                .enabled(userDetails.isEnabled())
                .accountLocked(!userDetails.isAccountNonLocked())
                .lastLoginAt(Instant.now())
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

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    /**
     * Register new user
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Create user
        UserEntity user = UserEntity.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .emailVerified(false)
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .build();

        user = userRepository.save(user);

        // Assign default role (BUSINESS_USER)
        RoleEntity defaultRole = roleRepository.findByName("BUSINESS_USER")
            .orElseThrow(() -> new RuntimeException("Default role BUSINESS_USER not found"));

        UserRoleEntity userRole = UserRoleEntity.builder()
            .user(user)
            .role(defaultRole)
            .build();

        userRoleRepository.save(userRole);

        log.info("User registered successfully: {}", user.getEmail());

        UserDto userDto = UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .emailVerified(user.getEmailVerified())
            .enabled(user.getEnabled())
            .accountLocked(user.getAccountLocked())
            .createdAt(user.getCreatedAt())
            .build();

        return RegisterResponse.builder()
            .user(userDto)
            .message("Registration successful. Please verify your email.")
            .emailVerificationSent(false) // TODO: Implement email verification
            .build();
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Check token type
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Invalid token type. Expected refresh token.");
        }

        // Check if token exists and is not revoked
        String tokenHash = hashToken(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findValidTokenByHash(tokenHash, Instant.now())
            .orElseThrow(() -> new TokenExpiredException("Refresh token expired or revoked"));

        // Get user email from token
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        UserEntity user = userRepository.findByEmailWithRolesAndPermissions(email)
            .orElseThrow(() -> new InvalidTokenException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Generate new tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId().toString());

        List<String> authorities = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        String newAccessToken = jwtTokenProvider.generateAccessToken(email, authorities, claims);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        // Revoke old refresh token
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        // Store new refresh token
        storeRefreshToken(user.getId(), newRefreshToken);

        // Update last used
        storedToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        return RefreshTokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
            .build();
    }

    /**
     * Logout user (revoke refresh token)
     */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("User logged out: {}", userId);
    }

    /**
     * Store refresh token in database
     */
    private void storeRefreshToken(UUID userId, String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        Date expiration = jwtTokenProvider.getExpirationFromToken(refreshToken);

        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
            .user(userRepository.findById(userId).orElseThrow())
            .tokenHash(tokenHash)
            .expiresAt(expiration.toInstant())
            .revoked(false)
            .build();

        refreshTokenRepository.save(tokenEntity);
    }

    /**
     * Hash token using SHA-256
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
