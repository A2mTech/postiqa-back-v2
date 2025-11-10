package fr.postiqa.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically log activity for a method.
 * When applied to a use case or service method, the activity will be logged automatically.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {

    /**
     * Action name for the activity log (e.g., "MEMBER_INVITED", "MEMBER_CREATED").
     * If not specified, will be derived from method name.
     */
    String value() default "";

    /**
     * Resource type for the activity log (e.g., "MEMBER", "INVITATION").
     */
    String resourceType();

    /**
     * Whether to extract resource ID from return value or parameters.
     * Default is to extract from the first UUID parameter.
     */
    boolean extractResourceId() default true;
}
