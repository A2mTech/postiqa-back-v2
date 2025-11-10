package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.RefreshTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.RefreshTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.gateway.auth.jwt.JwtTokenProvider;
import fr.postiqa.shared.dto.auth.RefreshTokenRequest;
import fr.postiqa.shared.dto.auth.RefreshTokenResponse;
import fr.postiqa.shared.exception.auth.InvalidTokenException;
import fr.postiqa.shared.exception.auth.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Refresh Token Use Case.
 * Single responsibility: Refresh access token using refresh token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StoreRefreshTokenUseCase storeRefreshTokenUseCase;

    @Transactional
    public RefreshTokenResponse execute(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        validateRefreshToken(refreshToken);

        // Get user from token
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

        // Revoke old refresh token and store new one
        revokeOldToken(refreshToken);
        storeRefreshTokenUseCase.execute(user.getId(), newRefreshToken);

        return RefreshTokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
            .build();
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Invalid token type. Expected refresh token.");
        }

        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findValidTokenByHash(tokenHash, Instant.now())
            .orElseThrow(() -> new TokenExpiredException("Refresh token expired or revoked"));
    }

    private void revokeOldToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Token not found"));

        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        storedToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(storedToken);
    }

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
