package org.example.appointment_system.aop;

import lombok.Getter;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.appointment_system.annotation.AutoFill;
import org.example.appointment_system.annotation.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AutoFillAspect.
 */
@ExtendWith(MockitoExtension.class)
class AutoFillAspectTest {

    @InjectMocks
    private AutoFillAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private Method method;

    /**
     * Test entity class with id, createdAt, and updatedAt fields.
     */
    @Getter
    @Setter
    public static class TestEntity {
        private Long id;
        private String name;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Non-entity class without id field.
     */
    @Getter
    @Setter
    public static class NonEntity {
        private String name;
    }

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
    }

    @Test
    @DisplayName("Should fill createdAt and updatedAt for INSERT operation")
    void shouldFillFieldsForInsert() throws Throwable {
        // Arrange
        TestEntity entity = new TestEntity();
        entity.setId(null);
        entity.setName("Test");

        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.INSERT);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{entity});
        when(joinPoint.proceed()).thenReturn(entity);

        // Act
        Object result = aspect.autoFill(joinPoint);

        // Assert
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity, result);
    }

    @Test
    @DisplayName("Should fill only updatedAt for UPDATE operation")
    void shouldFillOnlyUpdatedAtForUpdate() throws Throwable {
        // Arrange
        LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(1);
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setName("Test");
        entity.setCreatedAt(existingCreatedAt);
        entity.setUpdatedAt(null);

        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.UPDATE);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{entity});
        when(joinPoint.proceed()).thenReturn(entity);

        // Act
        Object result = aspect.autoFill(joinPoint);

        // Assert
        assertEquals(existingCreatedAt, entity.getCreatedAt(), "createdAt should not change");
        assertNotNull(entity.getUpdatedAt(), "updatedAt should be set");
        assertEquals(entity, result);
    }

    @Test
    @DisplayName("Should not override existing timestamps")
    void shouldNotOverrideExistingTimestamps() throws Throwable {
        // Arrange
        LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime existingUpdatedAt = LocalDateTime.now().minusDays(1);
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setCreatedAt(existingCreatedAt);
        entity.setUpdatedAt(existingUpdatedAt);

        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.UPDATE);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{entity});
        when(joinPoint.proceed()).thenReturn(entity);

        // Act
        aspect.autoFill(joinPoint);

        // Assert
        assertEquals(existingCreatedAt, entity.getCreatedAt());
        assertEquals(existingUpdatedAt, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null argument gracefully")
    void shouldHandleNullArgument() throws Throwable {
        // Arrange
        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.INSERT);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{null});
        when(joinPoint.proceed()).thenReturn(null);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> aspect.autoFill(joinPoint));
    }

    @Test
    @DisplayName("Should handle non-entity arguments")
    void shouldHandleNonEntityArgument() throws Throwable {
        // Arrange
        NonEntity nonEntity = new NonEntity();
        nonEntity.setName("Test");

        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.INSERT);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{nonEntity});
        when(joinPoint.proceed()).thenReturn(nonEntity);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> aspect.autoFill(joinPoint));
    }

    @Test
    @DisplayName("Should handle empty args array")
    void shouldHandleEmptyArgsArray() throws Throwable {
        // Arrange
        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.INSERT);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn(null);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> aspect.autoFill(joinPoint));
    }

    @Test
    @DisplayName("Should handle multiple entity arguments")
    void shouldHandleMultipleEntityArguments() throws Throwable {
        // Arrange
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);
        TestEntity entity2 = new TestEntity();
        entity2.setId(2L);

        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.UPDATE);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{entity1, entity2});
        when(joinPoint.proceed()).thenReturn(entity1);

        // Act
        aspect.autoFill(joinPoint);

        // Assert
        assertNotNull(entity1.getUpdatedAt());
        assertNotNull(entity2.getUpdatedAt());
    }

    @Test
    @DisplayName("Should proceed with join point after filling fields")
    void shouldProceedWithJoinPoint() throws Throwable {
        // Arrange
        TestEntity entity = new TestEntity();
        entity.setId(1L);

        AutoFill autoFill = mock(AutoFill.class);
        when(autoFill.value()).thenReturn(OperationType.UPDATE);
        when(method.getAnnotation(AutoFill.class)).thenReturn(autoFill);
        when(joinPoint.getArgs()).thenReturn(new Object[]{entity});
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        Object result = aspect.autoFill(joinPoint);

        // Assert
        verify(joinPoint).proceed();
        assertEquals("result", result);
    }
}
