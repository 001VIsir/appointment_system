package org.example.appointment_system.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OperationType enum.
 */
class OperationTypeTest {

    @Test
    @DisplayName("Should have INSERT operation type")
    void shouldHaveInsertOperation() {
        assertEquals("INSERT", OperationType.INSERT.name());
    }

    @Test
    @DisplayName("Should have UPDATE operation type")
    void shouldHaveUpdateOperation() {
        assertEquals("UPDATE", OperationType.UPDATE.name());
    }

    @Test
    @DisplayName("Should have exactly two operation types")
    void shouldHaveTwoOperationTypes() {
        OperationType[] types = OperationType.values();
        assertEquals(2, types.length);
        assertTrue(containsType(types, OperationType.INSERT));
        assertTrue(containsType(types, OperationType.UPDATE));
    }

    @Test
    @DisplayName("Should be able to get type by name")
    void shouldBeAbleToGetTypeByName() {
        assertEquals(OperationType.INSERT, OperationType.valueOf("INSERT"));
        assertEquals(OperationType.UPDATE, OperationType.valueOf("UPDATE"));
    }

    private boolean containsType(OperationType[] types, OperationType type) {
        for (OperationType t : types) {
            if (t == type) return true;
        }
        return false;
    }
}
