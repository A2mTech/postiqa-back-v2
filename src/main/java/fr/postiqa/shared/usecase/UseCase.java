package fr.postiqa.shared.usecase;

/**
 * Base interface for all use cases in the system.
 * Provides a unified contract for use case execution with input and output types.
 *
 * <p>Use cases implementing this interface will be automatically intercepted by
 * the UseCaseHandler aspect for logging, audit trail, and performance monitoring.
 *
 * @param <I> Input type (use Void for no input)
 * @param <O> Output type (use Void for no output)
 */
public interface UseCase<I, O> {

    /**
     * Execute the use case with the given input.
     *
     * @param input The input for the use case (can be null if I is Void)
     * @return The result of the use case execution
     * @throws Exception if the use case execution fails
     */
    O execute(I input) throws Exception;
}
