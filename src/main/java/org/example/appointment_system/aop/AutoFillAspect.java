package org.example.appointment_system.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.appointment_system.annotation.AutoFill;
import org.example.appointment_system.annotation.OperationType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * Aspect for automatically filling common entity fields like createdAt and updatedAt.
 * This eliminates boilerplate code in entities and ensures consistent timestamp handling.
 */
@Slf4j
@Aspect
@Component
public class AutoFillAspect {

    /**
     * Pointcut for methods annotated with @AutoFill.
     */
    @Pointcut("@annotation(org.example.appointment_system.annotation.AutoFill)")
    public void autoFillPointcut() {
    }

    /**
     * Around advice that fills common fields before the method execution.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("autoFillPointcut()")
    public Object autoFill(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("AutoFill aspect triggered for: {}", joinPoint.getSignature());

        // Get the annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // Process method arguments (entity objects)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg != null && isEntity(arg)) {
                    fillFields(arg, operationType);
                }
            }
        }

        return joinPoint.proceed();
    }

    /**
     * Check if the object looks like an entity (has id field).
     */
    private boolean isEntity(Object obj) {
        try {
            Field idField = obj.getClass().getDeclaredField("id");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * Fill the common fields based on operation type.
     */
    private void fillFields(Object entity, OperationType operationType) {
        LocalDateTime now = LocalDateTime.now();

        try {
            if (operationType == OperationType.INSERT) {
                // Fill createdAt for new entities
                setFieldIfExists(entity, "createdAt", now);
            }

            // Always fill updatedAt for both insert and update
            setFieldIfExists(entity, "updatedAt", now);

            log.debug("Auto-filled {} fields for entity: {}",
                    operationType, entity.getClass().getSimpleName());

        } catch (Exception e) {
            log.warn("Failed to auto-fill fields for entity {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Set a field value using reflection if the field exists.
     */
    private void setFieldIfExists(Object entity, String fieldName, Object value) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                // Only set if null (don't override existing values)
                Object currentValue = field.get(entity);
                if (currentValue == null) {
                    field.set(entity, value);
                }
            }
        } catch (Exception e) {
            log.trace("Could not set field '{}' on {}: {}", fieldName,
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Find a field in the class hierarchy.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}
