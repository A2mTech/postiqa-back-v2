package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Logout Use Case.
 * Single responsibility: Revoke all refresh tokens for a user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void execute(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("User logged out: {}", userId);
    }
}
