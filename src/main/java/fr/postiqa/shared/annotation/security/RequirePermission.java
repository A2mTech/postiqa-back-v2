package fr.postiqa.shared.annotation.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to check if user has specific permission.
 * Usage: @RequirePermission(resource = "POST", action = "CREATE")
 *
 * This will check if the authenticated user has the permission "POST:CREATE"
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * Resource name (e.g., POST, CLIENT, ANALYTICS)
     */
    String resource();

    /**
     * Action name (e.g., CREATE, VIEW, DELETE, MANAGE)
     */
    String action();

    /**
     * Optional message to display on access denied
     */
    String message() default "Insufficient permissions";
}
