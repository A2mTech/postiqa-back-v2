package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.RefreshTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.gateway.auth.jwt.JwtTokenProvider;
import fr.postiqa.shared.dto.auth.LoginRequest;
import fr.postiqa.shared.dto.auth.LoginResponse;
import fr.postiqa.shared.dto.auth.UserDto;
import fr.postiqa.shared.exception.auth.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Login Use Case.
 * Single responsibility: Authenticate user and return JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final StoreRefreshTokenUseCase storeRefreshTokenUseCase;

    @Transactional
    public LoginResponse execute(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Generate tokens with claims
            Map<String, Object> claims = buildClaims(userDetails);
            List<String> authorities = extractAuthorities(authentication);

            String accessToken = jwtTokenProvider.generateAccessToken(
                userDetails.getUsername(),
                authorities,
                claims
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUsername());

            // Store refresh token
            storeRefreshTokenUseCase.execute(userDetails.getId(), refreshToken);

            // Update last login
            updateLastLogin(userDetails.getId());

            // Build response
            return buildLoginResponse(accessToken, refreshToken, userDetails, authorities);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    private Map<String, Object> buildClaims(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userDetails.getId().toString());

        if (!userDetails.getScopes().isEmpty()) {
            CustomUserDetails.ScopeInfo primaryScope = userDetails.getScopes().get(0);
            if (primaryScope.getOrganizationId() != null) {
                claims.put("organization_id", primaryScope.getOrganizationId().toString());
            }
            if (primaryScope.getClientId() != null) {
                claims.put("client_id", primaryScope.getClientId().toString());
            }
        }
        return claims;
    }

    private List<String> extractAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    }

    private void updateLastLogin(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        user.setLastLoginAt(Instant.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken,
                                            CustomUserDetails userDetails, List<String> authorities) {
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
    }
}
