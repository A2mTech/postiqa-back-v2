package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.PasswordResetTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.PasswordResetTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.email.EmailService;
import fr.postiqa.shared.dto.auth.ForgotPasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Forgot Password Use Case.
 * Single responsibility: Generate password reset token and send reset email.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${mail.password-reset.url}")
    private String passwordResetBaseUrl;

    @Value("${mail.password-reset.token-validity-hours}")
    private int tokenValidityHours;

    @Transactional
    public void execute(ForgotPasswordRequest request) {
        // Find user by email - fail silently if not found (security best practice)
        userRepository.findByEmailIgnoreCase(request.getEmail())
            .ifPresent(user -> {
                // Invalidate all previous reset tokens for this user
                tokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

                // Generate new reset token
                String token = UUID.randomUUID().toString();
                Instant expiresAt = Instant.now().plus(tokenValidityHours, ChronoUnit.HOURS);

                PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(expiresAt)
                    .used(false)
                    .build();

                tokenRepository.save(resetToken);

                // Send password reset email
                String resetUrl = passwordResetBaseUrl + "?token=" + token;
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetUrl);

                log.info("Password reset token generated for user: {}", user.getEmail());
            });

        // Always return success to prevent email enumeration attacks
        log.info("Password reset request processed for email: {}", request.getEmail());
    }
}
