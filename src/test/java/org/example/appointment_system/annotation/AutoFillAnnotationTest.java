package org.example.appointment_system.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutoFill annotation.
 */
class AutoFillAnnotationTest {

    @AutoFill(OperationType.INSERT)
    public void insertMethod() {
    }

    @AutoFill(OperationType.UPDATE)
    public void updateMethod() {
    }

    @Test
    @DisplayName("Should have AutoFill annotation with INSERT type")
    void shouldHaveInsertAnnotation() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("insertMethod");
        AutoFill annotation = method.getAnnotation(AutoFill.class);

        assertNotNull(annotation);
        assertEquals(OperationType.INSERT, annotation.value());
    }

    @Test
    @DisplayName("Should have AutoFill annotation with UPDATE type")
    void shouldHaveUpdateAnnotation() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("updateMethod");
        AutoFill annotation = method.getAnnotation(AutoFill.class);

        assertNotNull(annotation);
        assertEquals(OperationType.UPDATE, annotation.value());
    }

    @Test
    @DisplayName("Should have correct retention policy")
    void shouldHaveCorrectRetentionPolicy() {
        assertTrue(AutoFill.class.isAnnotationPresent(java.lang.annotation.Retention.class));
        java.lang.annotation.Retention retention =
                AutoFill.class.getAnnotation(java.lang.annotation.Retention.class);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    @DisplayName("Should target methods")
    void shouldTargetMethods() {
        assertTrue(AutoFill.class.isAnnotationPresent(java.lang.annotation.Target.class));
        java.lang.annotation.Target target =
                AutoFill.class.getAnnotation(java.lang.annotation.Target.class);
        assertArrayEquals(new java.lang.annotation.ElementType[]{
                java.lang.annotation.ElementType.METHOD}, target.value());
    }
}
