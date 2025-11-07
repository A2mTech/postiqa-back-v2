package fr.postiqa.core.infrastructure.workflow.resilience;

import fr.postiqa.core.domain.workflow.model.RetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Handles retry logic with exponential backoff.
 * Implements resilience pattern for transient failures.
 */
@Component
@Slf4j
public class RetryHandler {

    /**
     * Execute a callable with retry logic
     *
     * @param operation The operation to execute
     * @param retryPolicy The retry policy to apply
     * @param operationName Name for logging
     * @param <T> Return type
     * @return The result of the operation
     * @throws Exception if all retry attempts fail
     */
    public <T> T executeWithRetry(
        Callable<T> operation,
        RetryPolicy retryPolicy,
        String operationName
    ) throws Exception {
        if (!retryPolicy.isEnabled()) {
            log.debug("Retry disabled for operation: {}", operationName);
            return operation.call();
        }

        int attempt = 0;
        Exception lastException = null;

        while (attempt <= retryPolicy.maxAttempts()) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} for operation: {}", attempt, operationName);
                }

                return operation.call();

            } catch (Exception e) {
                lastException = e;

                if (!retryPolicy.shouldRetry(e)) {
                    log.warn("Exception not retryable for operation {}: {}",
                        operationName, e.getClass().getSimpleName());
                    throw e;
                }

                if (attempt >= retryPolicy.maxAttempts()) {
                    log.error("Max retry attempts ({}) exceeded for operation: {}",
                        retryPolicy.maxAttempts(), operationName);
                    break;
                }

                Duration delay = retryPolicy.calculateDelay(attempt);
                log.warn("Operation {} failed (attempt {}), retrying in {}ms: {}",
                    operationName, attempt + 1, delay.toMillis(), e.getMessage());

                sleep(delay);
                attempt++;
            }
        }

        throw lastException;
    }

    /**
     * Sleep for a duration (interruptible)
     */
    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry sleep interrupted", e);
        }
    }

    /**
     * Check if an exception is retryable according to policy
     */
    public boolean isRetryable(Exception exception, RetryPolicy retryPolicy) {
        return retryPolicy.shouldRetry(exception);
    }

    /**
     * Calculate the next retry delay
     */
    public Duration getNextDelay(int attemptNumber, RetryPolicy retryPolicy) {
        return retryPolicy.calculateDelay(attemptNumber);
    }
}
