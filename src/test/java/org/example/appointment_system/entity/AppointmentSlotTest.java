package org.example.appointment_system.entity;

import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AppointmentSlot entity.
 */
class AppointmentSlotTest {

    private AppointmentTask createTestTask() {
        User user = new User("merchant1", "hashedpassword", "merchant@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Test Business");
        ServiceItem service = new ServiceItem(merchant, "Test Service", ServiceCategory.GENERAL, 30, BigDecimal.ZERO);
        return new AppointmentTask(service, "Test Task", LocalDate.of(2026, 2, 20), 10);
    }

    @Test
    void testAppointmentSlotCreationWithRequiredFields() {
        // Arrange
        AppointmentTask task = createTestTask();
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(10, 0);

        // Act
        AppointmentSlot slot = new AppointmentSlot(task, startTime, endTime, 5);

        // Assert
        assertEquals(task, slot.getTask());
        assertEquals(startTime, slot.getStartTime());
        assertEquals(endTime, slot.getEndTime());
        assertEquals(5, slot.getCapacity());
        assertNull(slot.getId());
        assertNull(slot.getCreatedAt());
        assertNull(slot.getUpdatedAt());
    }

    @Test
    void testAppointmentSlotCreationWithFullConstructor() {
        // Arrange
        AppointmentTask task = createTestTask();
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        // Act
        AppointmentSlot slot = new AppointmentSlot(task, startTime, endTime, 10, 3);

        // Assert
        assertEquals(task, slot.getTask());
        assertEquals(startTime, slot.getStartTime());
        assertEquals(endTime, slot.getEndTime());
        assertEquals(10, slot.getCapacity());
        assertEquals(3, slot.getBookedCount());
    }

    @Test
    void testAppointmentSlotCreationWithNoArgsConstructor() {
        // Arrange & Act
        AppointmentSlot slot = new AppointmentSlot();

        // Assert
        assertNull(slot.getId());
        assertNull(slot.getTask());
        assertNull(slot.getStartTime());
        assertNull(slot.getEndTime());
        assertNull(slot.getCreatedAt());
        assertNull(slot.getUpdatedAt());
        // Check defaults
        assertEquals(1, slot.getCapacity());
        assertEquals(0, slot.getBookedCount());
    }

    @Test
    void testDefaultValues() {
        // Arrange & Act
        AppointmentSlot slot = new AppointmentSlot();

        // Assert - check default values
        assertEquals(1, slot.getCapacity());
        assertEquals(0, slot.getBookedCount());
    }

    @Test
    void testPrePersistSetsTimestamps() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();

        // Act
        slot.onCreate();

        // Assert
        assertNotNull(slot.getCreatedAt());
        assertNotNull(slot.getUpdatedAt());
        assertEquals(slot.getCreatedAt(), slot.getUpdatedAt());
    }

    @Test
    void testPreUpdateSetsUpdatedAt() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        slot.onCreate();
        LocalDateTime originalCreatedAt = slot.getCreatedAt();

        // Wait a tiny bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        // Act
        slot.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, slot.getCreatedAt()); // createdAt should not change
        assertNotNull(slot.getUpdatedAt());
        assertTrue(slot.getUpdatedAt().isAfter(originalCreatedAt) || slot.getUpdatedAt().equals(originalCreatedAt));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        AppointmentTask task = createTestTask();
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        // Act
        slot.setId(1L);
        slot.setTask(task);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(20);
        slot.setBookedCount(5);
        slot.setCreatedAt(LocalDateTime.now().minusDays(1));
        slot.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals(1L, slot.getId());
        assertEquals(task, slot.getTask());
        assertEquals(startTime, slot.getStartTime());
        assertEquals(endTime, slot.getEndTime());
        assertEquals(20, slot.getCapacity());
        assertEquals(5, slot.getBookedCount());
        assertNotNull(slot.getCreatedAt());
        assertNotNull(slot.getUpdatedAt());
    }

    @Test
    void testTaskAssociation() {
        // Arrange
        AppointmentTask task = createTestTask();
        task.setId(100L);

        // Act
        AppointmentSlot slot = new AppointmentSlot(task, LocalTime.of(9, 0), LocalTime.of(10, 0), 5);

        // Assert
        assertNotNull(slot.getTask());
        assertEquals(100L, slot.getTask().getId());
        assertEquals("Test Task", slot.getTask().getTitle());
    }

    @Test
    void testHasAvailableCapacity() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        slot.setCapacity(5);

        // Act & Assert
        slot.setBookedCount(0);
        assertTrue(slot.hasAvailableCapacity());

        slot.setBookedCount(3);
        assertTrue(slot.hasAvailableCapacity());

        slot.setBookedCount(5);
        assertFalse(slot.hasAvailableCapacity());
    }

    @Test
    void testGetAvailableCapacity() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        slot.setCapacity(10);

        // Act & Assert
        slot.setBookedCount(0);
        assertEquals(10, slot.getAvailableCapacity());

        slot.setBookedCount(5);
        assertEquals(5, slot.getAvailableCapacity());

        slot.setBookedCount(10);
        assertEquals(0, slot.getAvailableCapacity());
    }

    @Test
    void testIncrementBookedCount() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        slot.setCapacity(3);
        slot.setBookedCount(0);

        // Act & Assert
        assertTrue(slot.incrementBookedCount());
        assertEquals(1, slot.getBookedCount());

        assertTrue(slot.incrementBookedCount());
        assertEquals(2, slot.getBookedCount());

        assertTrue(slot.incrementBookedCount());
        assertEquals(3, slot.getBookedCount());

        // Should fail when full
        assertFalse(slot.incrementBookedCount());
        assertEquals(3, slot.getBookedCount());
    }

    @Test
    void testDecrementBookedCount() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        slot.setCapacity(3);
        slot.setBookedCount(3);

        // Act & Assert
        assertTrue(slot.decrementBookedCount());
        assertEquals(2, slot.getBookedCount());

        assertTrue(slot.decrementBookedCount());
        assertEquals(1, slot.getBookedCount());

        assertTrue(slot.decrementBookedCount());
        assertEquals(0, slot.getBookedCount());

        // Should fail when already at 0
        assertFalse(slot.decrementBookedCount());
        assertEquals(0, slot.getBookedCount());
    }

    @Test
    void testTimeHandling() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        LocalTime morning = LocalTime.of(9, 0);
        LocalTime afternoon = LocalTime.of(14, 30);
        LocalTime evening = LocalTime.of(18, 0);

        // Act & Assert
        slot.setStartTime(morning);
        slot.setEndTime(LocalTime.of(10, 0));
        assertEquals(morning, slot.getStartTime());
        assertEquals(LocalTime.of(10, 0), slot.getEndTime());

        slot.setStartTime(afternoon);
        slot.setEndTime(evening);
        assertEquals(afternoon, slot.getStartTime());
        assertEquals(evening, slot.getEndTime());
    }

    @Test
    void testCapacityValues() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();

        // Act & Assert - test various capacities
        slot.setCapacity(1);
        assertEquals(1, slot.getCapacity());

        slot.setCapacity(50);
        assertEquals(50, slot.getCapacity());

        slot.setCapacity(100);
        assertEquals(100, slot.getCapacity());
    }

    @Test
    void testBookedCountValues() {
        // Arrange
        AppointmentSlot slot = new AppointmentSlot();
        slot.setCapacity(100);

        // Act & Assert
        slot.setBookedCount(0);
        assertEquals(0, slot.getBookedCount());

        slot.setBookedCount(50);
        assertEquals(50, slot.getBookedCount());

        slot.setBookedCount(100);
        assertEquals(100, slot.getBookedCount());
    }

    @Test
    void testAvailableCapacityCalculation() {
        // Arrange
        AppointmentTask task = createTestTask();
        AppointmentSlot slot = new AppointmentSlot(task, LocalTime.of(9, 0), LocalTime.of(10, 0), 10);

        // Initially empty
        assertEquals(10, slot.getAvailableCapacity());
        assertTrue(slot.hasAvailableCapacity());

        // Book some slots
        slot.incrementBookedCount();
        slot.incrementBookedCount();
        slot.incrementBookedCount();
        assertEquals(7, slot.getAvailableCapacity());
        assertTrue(slot.hasAvailableCapacity());

        // Fill the slot
        for (int i = 0; i < 7; i++) {
            slot.incrementBookedCount();
        }
        assertEquals(0, slot.getAvailableCapacity());
        assertFalse(slot.hasAvailableCapacity());
    }

    @Test
    void testSlotWithSingleCapacity() {
        // Arrange
        AppointmentTask task = createTestTask();
        AppointmentSlot slot = new AppointmentSlot(task, LocalTime.of(9, 0), LocalTime.of(9, 30), 1);

        // Act & Assert
        assertEquals(1, slot.getCapacity());
        assertEquals(0, slot.getBookedCount());
        assertTrue(slot.hasAvailableCapacity());

        // Book it
        assertTrue(slot.incrementBookedCount());
        assertEquals(1, slot.getBookedCount());
        assertFalse(slot.hasAvailableCapacity());

        // Cannot book more
        assertFalse(slot.incrementBookedCount());
        assertEquals(1, slot.getBookedCount());
    }
}
