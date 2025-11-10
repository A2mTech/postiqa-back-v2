package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.PasswordResetTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.PasswordResetTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.email.EmailService;
import fr.postiqa.shared.dto.auth.ResetPasswordRequest;
import fr.postiqa.shared.exception.auth.InvalidTokenException;
import fr.postiqa.shared.exception.auth.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Reset Password Use Case.
 * Single responsibility: Validate reset token and update user password.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void execute(ResetPasswordRequest request) {
        // Find and validate token
        PasswordResetTokenEntity resetToken = tokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        // Check if token is already used
        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        // Check if token is expired
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        // Get user
        UserEntity user = resetToken.getUser();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // Reset failed login attempts
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);

        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);

        // Invalidate all other reset tokens for this user
        tokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

        // Send password changed confirmation email
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

        log.info("Password reset successful for user: {}", user.getEmail());
    }
}
