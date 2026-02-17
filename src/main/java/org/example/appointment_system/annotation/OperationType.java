package org.example.appointment_system.annotation;

/**
 * Enumeration for operation types used in auto-fill aspect.
 */
public enum OperationType {
    /**
     * Insert operation - fills createdAt and updatedAt.
     */
    INSERT,

    /**
     * Update operation - fills updatedAt only.
     */
    UPDATE
}
