package org.example.appointment_system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to mark methods that should have common fields auto-filled.
 * Apply this to repository save methods or service methods that create/update entities.
 *
 * Usage example:
 * <pre>
 * {@code
 * @AutoFill(OperationType.INSERT)
 * public Entity save(Entity entity) { ... }
 * }
 * </pre>
 */
@java.lang.annotation.Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    /**
     * The operation type that determines which fields to fill.
     *
     * @return the operation type
     */
    OperationType value();
}
