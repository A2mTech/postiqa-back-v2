package fr.postiqa.features.postmanagement.domain.exception;

/**
 * Base exception for all post management domain exceptions.
 * Domain exceptions represent business rule violations.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
