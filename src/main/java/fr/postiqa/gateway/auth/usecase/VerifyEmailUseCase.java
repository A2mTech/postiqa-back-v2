package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.EmailVerificationTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.EmailVerificationTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.shared.exception.auth.InvalidTokenException;
import fr.postiqa.shared.exception.auth.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Verify Email Use Case.
 * Single responsibility: Validate verification token and mark email as verified.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;

    @Transactional
    public void execute(String token) {
        // Find and validate token
        EmailVerificationTokenEntity verificationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid email verification token"));

        // Check if token is already used
        if (verificationToken.getUsed()) {
            throw new InvalidTokenException("Email verification token has already been used");
        }

        // Check if token is expired
        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Email verification token has expired");
        }

        // Get user
        UserEntity user = verificationToken.getUser();

        // Check if email is already verified
        if (user.getEmailVerified()) {
            log.info("Email already verified for user: {}", user.getEmail());
            return;
        }

        // Mark email as verified
        user.setEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);

        // Invalidate all other verification tokens for this user
        tokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

        log.info("Email verified successfully for user: {}", user.getEmail());
    }
}
