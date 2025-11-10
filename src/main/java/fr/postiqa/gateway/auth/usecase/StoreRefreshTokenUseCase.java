package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.RefreshTokenEntity;
import fr.postiqa.database.repository.RefreshTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Store Refresh Token Use Case.
 * Single responsibility: Store hashed refresh token in database.
 */
@Component
@RequiredArgsConstructor
public class StoreRefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void execute(UUID userId, String refreshToken) {
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
