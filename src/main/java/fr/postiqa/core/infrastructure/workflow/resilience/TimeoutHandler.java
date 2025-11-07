package fr.postiqa.core.infrastructure.workflow.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * Handles timeout logic for workflow step execution.
 * Wraps operations with configurable timeouts.
 */
@Component
@Slf4j
public class TimeoutHandler {

    private final ExecutorService timeoutExecutor;

    public TimeoutHandler() {
        this.timeoutExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("workflow-timeout-handler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Execute a callable with a timeout
     *
     * @param operation The operation to execute
     * @param timeout The timeout duration
     * @param operationName Name for logging
     * @param <T> Return type
     * @return The result of the operation
     * @throws TimeoutException if operation exceeds timeout
     * @throws Exception if operation fails
     */
    public <T> T executeWithTimeout(
        Callable<T> operation,
        Duration timeout,
        String operationName
    ) throws Exception {
        log.debug("Executing operation {} with timeout: {}", operationName, timeout);

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, timeoutExecutor);

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Operation {} timed out after {}", operationName, timeout);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException("Unexpected error during operation execution", cause);
        }
    }

    /**
     * Execute a runnable with a timeout (no return value)
     *
     * @param operation The operation to execute
     * @param timeout The timeout duration
     * @param operationName Name for logging
     * @throws TimeoutException if operation exceeds timeout
     */
    public void executeWithTimeout(
        Runnable operation,
        Duration timeout,
        String operationName
    ) throws Exception {
        executeWithTimeout(() -> {
            operation.run();
            return null;
        }, timeout, operationName);
    }

    /**
     * Shutdown the timeout executor
     */
    public void shutdown() {
        log.info("Shutting down timeout executor");
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
