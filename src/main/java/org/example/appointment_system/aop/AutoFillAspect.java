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
 * 用于自动填充常见实体字段（如createdAt和updatedAt）的切面。
 * 这消除了实体中的样板代码并确保一致的时间戳处理。
 */
@Slf4j
@Aspect
@Component
public class AutoFillAspect {

    /**
     * 带@AutoFill注解的方法的切点。
     */
    @Pointcut("@annotation(org.example.appointment_system.annotation.AutoFill)")
    public void autoFillPointcut() {
    }

    /**
     * 在方法执行前填充常见字段的环绕通知。
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 如果方法抛出异常
     */
    @Around("autoFillPointcut()")
    public Object autoFill(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("AutoFill aspect triggered for: {}", joinPoint.getSignature());

        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // 处理方法参数（实体对象）
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
     * 检查对象是否像实体（有id字段）。
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
     * 根据操作类型填充常见字段。
     */
    private void fillFields(Object entity, OperationType operationType) {
        LocalDateTime now = LocalDateTime.now();

        try {
            if (operationType == OperationType.INSERT) {
                // 为新实体填充createdAt
                setFieldIfExists(entity, "createdAt", now);
            }

            // 插入和更新都填充updatedAt
            setFieldIfExists(entity, "updatedAt", now);

            log.debug("Auto-filled {} fields for entity: {}",
                    operationType, entity.getClass().getSimpleName());

        } catch (Exception e) {
            log.warn("Failed to auto-fill fields for entity {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 使用反射在字段存在时设置字段值。
     */
    private void setFieldIfExists(Object entity, String fieldName, Object value) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                // 只在为空时设置（不覆盖现有值）
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
     * 在类层次结构中查找字段。
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
