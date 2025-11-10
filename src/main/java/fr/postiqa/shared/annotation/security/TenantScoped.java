package fr.postiqa.shared.annotation.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark endpoints as tenant-scoped (agency multi-tenant).
 * Endpoints with this annotation will enforce tenant isolation checks.
 * User can only access resources belonging to their organization/client scope.
 *
 * Usage: @TenantScoped
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantScoped {

    /**
     * Whether to enforce strict tenant checks (default: true)
     */
    boolean strict() default true;

    /**
     * Optional message to display on tenant access violation
     */
    String message() default "Access denied: resource belongs to another tenant";
}
