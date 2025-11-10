package fr.postiqa.shared.exception.auth;

/**
 * Exception thrown when attempting to create a cycle in the management hierarchy.
 */
public class HierarchyCycleException extends RuntimeException {

    public HierarchyCycleException(String message) {
        super(message);
    }

    public HierarchyCycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
