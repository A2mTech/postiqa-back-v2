package fr.postiqa.features.postmanagement.domain.exception;

import fr.postiqa.features.postmanagement.domain.vo.PostId;
import fr.postiqa.shared.enums.PostStatus;

/**
 * Exception thrown when an operation cannot be performed due to post status.
 */
public class InvalidPostStatusException extends DomainException {

    private final PostId postId;
    private final PostStatus currentStatus;
    private final String operation;

    public InvalidPostStatusException(PostId postId, PostStatus currentStatus, String operation) {
        super(String.format("Cannot %s post %s in status %s", operation, postId, currentStatus));
        this.postId = postId;
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public PostId getPostId() {
        return postId;
    }

    public PostStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getOperation() {
        return operation;
    }
}
