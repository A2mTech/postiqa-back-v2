package fr.postiqa.shared.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Meta-annotation for use cases that combines Spring's @Component with
 * automatic activity logging and audit trail capabilities.
 *
 * <p>Use cases annotated with @UseCase will automatically:
 * <ul>
 *   <li>Be registered as Spring beans</li>
 *   <li>Have their execution logged with performance metrics</li>
 *   <li>Create audit trail entries for compliance</li>
 *   <li>Track tenant context (user, organization, client)</li>
 *   <li>Capture errors and exceptions</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @UseCase(
 *     name = "CreatePost",
 *     resourceType = "POST",
 *     description = "Creates a new social media post"
 * )
 * public class CreatePostUseCase implements UseCase<CreatePostCommand, PostId> {
 *     @Override
 *     public PostId execute(CreatePostCommand input) {
 *         // implementation
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UseCase {

    /**
     * The use case name for logging and identification.
     * If not specified, will be derived from the class name.
     *
     * <p>Example: "CreatePost", "InviteMember", "GenerateContent"
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * Resource type that this use case operates on (e.g., "POST", "MEMBER", "CONTENT").
     * Used for audit trail categorization.
     *
     * <p>If empty, activity logging will be disabled for this use case.
     */
    String resourceType() default "";

    /**
     * Human-readable description of what this use case does.
     * Used for documentation and monitoring dashboards.
     */
    String description() default "";

    /**
     * Whether this use case should automatically log activity to the audit trail.
     * Default is true if resourceType is specified, false otherwise.
     */
    boolean logActivity() default true;

    /**
     * Whether to extract resource ID from the use case input or output.
     * If true, the handler will attempt to find a UUID field in the input/output
     * and log it as the resource ID.
     */
    boolean extractResourceId() default true;
}
