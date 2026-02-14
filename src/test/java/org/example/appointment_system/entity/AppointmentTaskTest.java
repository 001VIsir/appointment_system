package org.example.appointment_system.entity;

import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AppointmentTask entity.
 */
class AppointmentTaskTest {

    private ServiceItem createTestServiceItem() {
        User user = new User("merchant1", "hashedpassword", "merchant@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Test Business");
        return new ServiceItem(merchant, "Test Service", ServiceCategory.GENERAL, 30, BigDecimal.ZERO);
    }

    @Test
    void testAppointmentTaskCreationWithRequiredFields() {
        // Arrange
        ServiceItem service = createTestServiceItem();
        LocalDate taskDate = LocalDate.of(2026, 2, 20);

        // Act
        AppointmentTask task = new AppointmentTask(service, "Morning Appointments", taskDate, 10);

        // Assert
        assertEquals(service, task.getService());
        assertEquals("Morning Appointments", task.getTitle());
        assertEquals(taskDate, task.getTaskDate());
        assertEquals(10, task.getTotalCapacity());
        assertNull(task.getDescription());
        assertNull(task.getId());
        assertNull(task.getCreatedAt());
        assertNull(task.getUpdatedAt());
    }

    @Test
    void testAppointmentTaskCreationWithFullConstructor() {
        // Arrange
        ServiceItem service = createTestServiceItem();
        LocalDate taskDate = LocalDate.of(2026, 2, 20);

        // Act
        AppointmentTask task = new AppointmentTask(
            service,
            "Afternoon Appointments",
            "Available slots from 2pm to 6pm",
            taskDate,
            20,
            true
        );

        // Assert
        assertEquals(service, task.getService());
        assertEquals("Afternoon Appointments", task.getTitle());
        assertEquals("Available slots from 2pm to 6pm", task.getDescription());
        assertEquals(taskDate, task.getTaskDate());
        assertEquals(20, task.getTotalCapacity());
        assertTrue(task.getActive());
    }

    @Test
    void testAppointmentTaskCreationWithNoArgsConstructor() {
        // Arrange & Act
        AppointmentTask task = new AppointmentTask();

        // Assert
        assertNull(task.getId());
        assertNull(task.getService());
        assertNull(task.getTitle());
        assertNull(task.getDescription());
        assertNull(task.getTaskDate());
        assertNull(task.getCreatedAt());
        assertNull(task.getUpdatedAt());
        // Check defaults
        assertEquals(1, task.getTotalCapacity());
        assertTrue(task.getActive());
    }

    @Test
    void testDefaultValues() {
        // Arrange & Act
        AppointmentTask task = new AppointmentTask();

        // Assert - check default values
        assertEquals(1, task.getTotalCapacity());
        assertTrue(task.getActive());
    }

    @Test
    void testPrePersistSetsTimestamps() {
        // Arrange
        AppointmentTask task = new AppointmentTask();

        // Act
        task.onCreate();

        // Assert
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
        assertEquals(task.getCreatedAt(), task.getUpdatedAt());
    }

    @Test
    void testPreUpdateSetsUpdatedAt() {
        // Arrange
        AppointmentTask task = new AppointmentTask();
        task.onCreate();
        LocalDateTime originalCreatedAt = task.getCreatedAt();

        // Wait a tiny bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        // Act
        task.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, task.getCreatedAt()); // createdAt should not change
        assertNotNull(task.getUpdatedAt());
        assertTrue(task.getUpdatedAt().isAfter(originalCreatedAt) || task.getUpdatedAt().equals(originalCreatedAt));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        AppointmentTask task = new AppointmentTask();
        ServiceItem service = createTestServiceItem();
        LocalDate taskDate = LocalDate.of(2026, 3, 15);

        // Act
        task.setId(1L);
        task.setService(service);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setTaskDate(taskDate);
        task.setTotalCapacity(15);
        task.setActive(false);
        task.setCreatedAt(LocalDateTime.now().minusDays(1));
        task.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals(1L, task.getId());
        assertEquals(service, task.getService());
        assertEquals("Test Task", task.getTitle());
        assertEquals("Test Description", task.getDescription());
        assertEquals(taskDate, task.getTaskDate());
        assertEquals(15, task.getTotalCapacity());
        assertFalse(task.getActive());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    void testServiceAssociation() {
        // Arrange
        ServiceItem service = createTestServiceItem();
        service.setId(100L);

        // Act
        AppointmentTask task = new AppointmentTask(service, "Task", LocalDate.now(), 5);

        // Assert
        assertNotNull(task.getService());
        assertEquals(100L, task.getService().getId());
        assertEquals("Test Service", task.getService().getName());
    }

    @Test
    void testActiveToggle() {
        // Arrange
        AppointmentTask task = new AppointmentTask();
        assertTrue(task.getActive()); // Default is true

        // Act
        task.setActive(false);

        // Assert
        assertFalse(task.getActive());

        // Act again
        task.setActive(true);

        // Assert
        assertTrue(task.getActive());
    }

    @Test
    void testTotalCapacityValues() {
        // Arrange
        AppointmentTask task = new AppointmentTask();

        // Act & Assert - test various capacities
        task.setTotalCapacity(1);
        assertEquals(1, task.getTotalCapacity());

        task.setTotalCapacity(50);
        assertEquals(50, task.getTotalCapacity());

        task.setTotalCapacity(100);
        assertEquals(100, task.getTotalCapacity());
    }

    @Test
    void testTaskDateHandling() {
        // Arrange
        AppointmentTask task = new AppointmentTask();
        LocalDate futureDate = LocalDate.of(2026, 12, 31);
        LocalDate pastDate = LocalDate.of(2025, 1, 1);

        // Act & Assert
        task.setTaskDate(futureDate);
        assertEquals(futureDate, task.getTaskDate());

        task.setTaskDate(pastDate);
        assertEquals(pastDate, task.getTaskDate());
    }

    @Test
    void testNullableFieldsCanBeSetAfterCreation() {
        // Arrange
        ServiceItem service = createTestServiceItem();
        AppointmentTask task = new AppointmentTask(service, "Minimal Task", LocalDate.now(), 1);

        // Initially description is null
        assertNull(task.getDescription());

        // Act - set optional fields later
        task.setDescription("Updated description");

        // Assert
        assertEquals("Updated description", task.getDescription());
    }

    @Test
    void testTitleIsRequired() {
        // This test documents that title is required
        // The actual enforcement happens at the database level via @Column(nullable = false)

        // Arrange
        ServiceItem service = createTestServiceItem();

        // Act
        AppointmentTask task = new AppointmentTask(service, "Required Title", LocalDate.now(), 5);

        // Assert
        assertNotNull(task.getTitle());
        assertEquals("Required Title", task.getTitle());
    }

    @Test
    void testTaskDateIsRequired() {
        // This test documents that taskDate is required
        // The actual enforcement happens at the database level via @Column(nullable = false)

        // Arrange
        ServiceItem service = createTestServiceItem();
        LocalDate taskDate = LocalDate.of(2026, 5, 15);

        // Act
        AppointmentTask task = new AppointmentTask(service, "Task", taskDate, 5);

        // Assert
        assertNotNull(task.getTaskDate());
        assertEquals(taskDate, task.getTaskDate());
    }
}
