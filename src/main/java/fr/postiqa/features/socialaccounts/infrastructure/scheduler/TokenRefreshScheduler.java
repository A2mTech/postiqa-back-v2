package fr.postiqa.features.socialaccounts.infrastructure.scheduler;

import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.usecase.RefreshTokenUseCase;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler that automatically refreshes OAuth2 tokens that are expiring soon.
 * Runs every hour to check for tokens expiring within the next 24 hours.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshScheduler {

    private final SocialAccountPort socialAccountPort;
    private final RefreshTokenUseCase refreshTokenUseCase;

    /**
     * Refresh tokens that are expiring within the next 24 hours.
     * Runs every hour (3600000 milliseconds).
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000) // Every hour, initial delay 1 minute
    public void refreshExpiringTokens() {
        log.info("Starting scheduled token refresh job");

        try {
            // Find all accounts with tokens expiring in the next 24 hours
            Instant expirationThreshold = Instant.now().plusSeconds(86400); // 24 hours
            List<SocialAccount> expiringAccounts = socialAccountPort.findAccountsWithExpiringTokens(expirationThreshold);

            if (expiringAccounts.isEmpty()) {
                log.info("No tokens expiring soon");
                return;
            }

            log.info("Found {} tokens expiring soon, attempting to refresh", expiringAccounts.size());

            int successCount = 0;
            int failureCount = 0;

            for (SocialAccount account : expiringAccounts) {
                // Skip if no refresh token available
                if (account.getToken() == null || !account.getToken().hasRefreshToken()) {
                    log.warn("Account {} ({}) has no refresh token, skipping",
                        account.getId(), account.getPlatform());
                    failureCount++;
                    continue;
                }

                try {
                    refreshTokenUseCase.execute(account.getId());
                    successCount++;
                    log.info("Successfully refreshed token for account {} ({})",
                        account.getId(), account.getPlatform());
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to refresh token for account {} ({}): {}",
                        account.getId(), account.getPlatform(), e.getMessage());
                }
            }

            log.info("Token refresh job completed. Success: {}, Failures: {}", successCount, failureCount);

        } catch (Exception e) {
            log.error("Error during scheduled token refresh", e);
        }
    }

    /**
     * Handle expired tokens by marking accounts as inactive.
     * Runs every 6 hours (21600000 milliseconds).
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 300000) // Every 6 hours, initial delay 5 minutes
    public void handleExpiredTokens() {
        log.info("Starting expired tokens check");

        try {
            Instant now = Instant.now();
            List<SocialAccount> expiredAccounts = socialAccountPort.findAccountsWithExpiredTokens(now);

            if (expiredAccounts.isEmpty()) {
                log.info("No expired tokens found");
                return;
            }

            log.info("Found {} expired tokens", expiredAccounts.size());

            for (SocialAccount account : expiredAccounts) {
                // Try to refresh if refresh token is available
                if (account.getToken() != null && account.getToken().hasRefreshToken()) {
                    try {
                        refreshTokenUseCase.execute(account.getId());
                        log.info("Successfully refreshed expired token for account {} ({})",
                            account.getId(), account.getPlatform());
                        continue;
                    } catch (Exception e) {
                        log.warn("Failed to refresh expired token for account {} ({}), will deactivate",
                            account.getId(), account.getPlatform());
                    }
                }

                // Deactivate account if refresh failed or no refresh token
                try {
                    account.deactivate();
                    socialAccountPort.save(account);
                    log.info("Deactivated account {} ({}) due to expired token",
                        account.getId(), account.getPlatform());
                } catch (Exception e) {
                    log.error("Failed to deactivate account {} ({}): {}",
                        account.getId(), account.getPlatform(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error during expired tokens check", e);
        }
    }
}
