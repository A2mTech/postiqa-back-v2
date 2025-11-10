package fr.postiqa.features.postmanagement.domain.exception;

import fr.postiqa.features.postmanagement.domain.vo.UserId;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for.
 */
public class UnauthorizedAccessException extends DomainException {

    private final UserId userId;
    private final String resourceType;
    private final String resourceId;

    public UnauthorizedAccessException(UserId userId, String resourceType, String resourceId) {
        super(String.format("User %s is not authorized to access %s: %s", userId, resourceType, resourceId));
        this.userId = userId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public UnauthorizedAccessException(String message) {
        super(message);
        this.userId = null;
        this.resourceType = null;
        this.resourceId = null;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
