package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Validate Reset Token Use Case.
 * Single responsibility: Check if a password reset token is valid.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateResetTokenUseCase {

    private final PasswordResetTokenRepository tokenRepository;

    @Transactional(readOnly = true)
    public boolean execute(String token) {
        boolean isValid = tokenRepository.findValidTokenByToken(token, Instant.now()).isPresent();

        log.info("Reset token validation result: {}", isValid);

        return isValid;
    }
}
