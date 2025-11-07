package fr.postiqa.core.domain.enums;

/**
 * Execution modes for scraping operations
 */
public enum ExecutionMode {
    /**
     * Synchronous execution - blocks until result is available
     */
    SYNC,

    /**
     * Asynchronous execution in a separate thread using CompletableFuture
     */
    ASYNC_THREAD,

    /**
     * Native async execution using provider's job system (e.g., Apify runs)
     * Requires polling for completion
     */
    ASYNC_NATIVE;

    public boolean isAsync() {
        return this != SYNC;
    }

    public boolean requiresPolling() {
        return this == ASYNC_NATIVE;
    }
}
