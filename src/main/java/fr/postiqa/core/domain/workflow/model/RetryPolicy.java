package fr.postiqa.core.domain.workflow.model;

import java.time.Duration;
import java.util.Set;

/**
 * Retry policy configuration for workflow steps.
 * Defines how failures should be retried with backoff strategies.
 */
public record RetryPolicy(
    int maxAttempts,
    Duration initialDelay,
    Duration maxDelay,
    double backoffMultiplier,
    Set<Class<? extends Exception>> retryableExceptions,
    Set<Class<? extends Exception>> nonRetryableExceptions
) {

    /**
     * No retry policy - fail immediately
     */
    public static RetryPolicy none() {
        return new RetryPolicy(
            0,
            Duration.ZERO,
            Duration.ZERO,
            1.0,
            Set.of(),
            Set.of()
        );
    }

    /**
     * Simple fixed delay retry
     */
    public static RetryPolicy fixedDelay(int maxAttempts, Duration delay) {
        return new RetryPolicy(
            maxAttempts,
            delay,
            delay,
            1.0, // No backoff
            Set.of(Exception.class), // Retry all exceptions
            Set.of()
        );
    }

    /**
     * Exponential backoff retry (recommended)
     */
    public static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialDelay) {
        return new RetryPolicy(
            maxAttempts,
            initialDelay,
            Duration.ofMinutes(5), // Max 5 minutes
            2.0, // Double each time
            Set.of(Exception.class),
            Set.of()
        );
    }

    /**
     * Custom retry policy with specific exceptions
     */
    public static RetryPolicy custom(
        int maxAttempts,
        Duration initialDelay,
        Duration maxDelay,
        double backoffMultiplier,
        Set<Class<? extends Exception>> retryableExceptions
    ) {
        return new RetryPolicy(
            maxAttempts,
            initialDelay,
            maxDelay,
            backoffMultiplier,
            retryableExceptions,
            Set.of()
        );
    }

    /**
     * Compact constructor with validation
     */
    public RetryPolicy {
        if (maxAttempts < 0) {
            throw new IllegalArgumentException("maxAttempts must be >= 0");
        }
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay cannot be negative");
        }
        if (maxDelay.isNegative()) {
            throw new IllegalArgumentException("maxDelay cannot be negative");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
        // Defensive copies
        retryableExceptions = Set.copyOf(retryableExceptions);
        nonRetryableExceptions = Set.copyOf(nonRetryableExceptions);
    }

    /**
     * Check if retry is enabled
     */
    public boolean isEnabled() {
        return maxAttempts > 0;
    }

    /**
     * Check if an exception should be retried
     */
    public boolean shouldRetry(Exception exception) {
        if (!isEnabled()) {
            return false;
        }

        // Check non-retryable exceptions first (blacklist)
        for (Class<? extends Exception> nonRetryable : nonRetryableExceptions) {
            if (nonRetryable.isInstance(exception)) {
                return false;
            }
        }

        // If retryable exceptions specified (whitelist), check membership
        if (!retryableExceptions.isEmpty()) {
            for (Class<? extends Exception> retryable : retryableExceptions) {
                if (retryable.isInstance(exception)) {
                    return true;
                }
            }
            return false;
        }

        // Default: retry all exceptions
        return true;
    }

    /**
     * Calculate delay for a given attempt number (0-based)
     */
    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("attemptNumber must be >= 0");
        }

        if (backoffMultiplier == 1.0) {
            return initialDelay; // Fixed delay
        }

        // Exponential backoff: initialDelay * (backoffMultiplier ^ attemptNumber)
        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber));
        Duration calculatedDelay = Duration.ofMillis(delayMillis);

        // Cap at maxDelay
        return calculatedDelay.compareTo(maxDelay) > 0 ? maxDelay : calculatedDelay;
    }
}
