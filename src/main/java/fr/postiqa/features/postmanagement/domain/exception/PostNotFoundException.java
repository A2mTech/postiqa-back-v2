package fr.postiqa.features.postmanagement.domain.exception;

import fr.postiqa.features.postmanagement.domain.vo.PostId;

/**
 * Exception thrown when a post is not found.
 */
public class PostNotFoundException extends DomainException {

    private final PostId postId;

    public PostNotFoundException(PostId postId) {
        super("Post not found: " + postId);
        this.postId = postId;
    }

    public PostNotFoundException(String postId) {
        super("Post not found: " + postId);
        this.postId = PostId.of(postId);
    }

    public PostId getPostId() {
        return postId;
    }
}
