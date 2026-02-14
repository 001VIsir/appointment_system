package org.example.appointment_system.entity;

import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Booking entity.
 */
class BookingTest {

    private User createTestUser() {
        return new User("testuser", "hashedpassword", "test@example.com", UserRole.USER);
    }

    private AppointmentSlot createTestSlot() {
        User merchant = new User("merchant1", "hashedpassword", "merchant@example.com", UserRole.MERCHANT);
        MerchantProfile profile = new MerchantProfile(merchant, "Test Business");
        ServiceItem service = new ServiceItem(profile, "Test Service", ServiceCategory.GENERAL, 30, BigDecimal.ZERO);
        AppointmentTask task = new AppointmentTask(service, "Test Task", LocalDate.of(2026, 2, 20), 10);
        return new AppointmentSlot(task, LocalTime.of(9, 0), LocalTime.of(10, 0), 5);
    }

    @Test
    void testBookingCreationWithRequiredFields() {
        // Arrange
        User user = createTestUser();
        AppointmentSlot slot = createTestSlot();

        // Act
        Booking booking = new Booking(user, slot);

        // Assert
        assertEquals(user, booking.getUser());
        assertEquals(slot, booking.getSlot());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertNull(booking.getId());
        assertNull(booking.getCreatedAt());
        assertNull(booking.getUpdatedAt());
    }

    @Test
    void testBookingCreationWithRemark() {
        // Arrange
        User user = createTestUser();
        AppointmentSlot slot = createTestSlot();
        String remark = "Please call before arrival";

        // Act
        Booking booking = new Booking(user, slot, remark);

        // Assert
        assertEquals(user, booking.getUser());
        assertEquals(slot, booking.getSlot());
        assertEquals(remark, booking.getRemark());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void testBookingCreationWithFullConstructor() {
        // Arrange
        User user = createTestUser();
        AppointmentSlot slot = createTestSlot();
        String remark = "Need wheelchair access";

        // Act
        Booking booking = new Booking(user, slot, BookingStatus.CONFIRMED, remark);

        // Assert
        assertEquals(user, booking.getUser());
        assertEquals(slot, booking.getSlot());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(remark, booking.getRemark());
    }

    @Test
    void testBookingCreationWithNoArgsConstructor() {
        // Arrange & Act
        Booking booking = new Booking();

        // Assert
        assertNull(booking.getId());
        assertNull(booking.getUser());
        assertNull(booking.getSlot());
        assertNull(booking.getRemark());
        assertNull(booking.getCreatedAt());
        assertNull(booking.getUpdatedAt());
        // Check defaults
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(0L, booking.getVersion());
    }

    @Test
    void testDefaultValues() {
        // Arrange & Act
        Booking booking = new Booking();

        // Assert - check default values
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(0L, booking.getVersion());
    }

    @Test
    void testPrePersistSetsTimestampsAndDefaultStatus() {
        // Arrange
        Booking booking = new Booking();
        booking.setStatus(null); // Clear default to test onCreate

        // Act
        booking.onCreate();

        // Assert
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getUpdatedAt());
        assertEquals(booking.getCreatedAt(), booking.getUpdatedAt());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingStatus() {
        // Arrange
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);

        // Act
        booking.onCreate();

        // Assert
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void testPreUpdateSetsUpdatedAt() {
        // Arrange
        Booking booking = new Booking();
        booking.onCreate();
        LocalDateTime originalCreatedAt = booking.getCreatedAt();

        // Wait a tiny bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        // Act
        booking.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, booking.getCreatedAt()); // createdAt should not change
        assertNotNull(booking.getUpdatedAt());
        assertTrue(booking.getUpdatedAt().isAfter(originalCreatedAt) || booking.getUpdatedAt().equals(originalCreatedAt));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        Booking booking = new Booking();
        User user = createTestUser();
        AppointmentSlot slot = createTestSlot();

        // Act
        booking.setId(1L);
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setRemark("Test remark");
        booking.setVersion(2L);
        booking.setCreatedAt(LocalDateTime.now().minusDays(1));
        booking.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals(1L, booking.getId());
        assertEquals(user, booking.getUser());
        assertEquals(slot, booking.getSlot());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals("Test remark", booking.getRemark());
        assertEquals(2L, booking.getVersion());
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getUpdatedAt());
    }

    @Test
    void testConfirmFromPending() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.PENDING);

        // Act
        boolean result = booking.confirm();

        // Assert
        assertTrue(result);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void testConfirmFromConfirmedFails() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.CONFIRMED);

        // Act
        boolean result = booking.confirm();

        // Assert
        assertFalse(result);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void testConfirmFromCancelledFails() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.CANCELLED);

        // Act
        boolean result = booking.confirm();

        // Assert
        assertFalse(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void testCancelFromPending() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.PENDING);

        // Act
        boolean result = booking.cancel();

        // Assert
        assertTrue(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void testCancelFromConfirmed() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.CONFIRMED);

        // Act
        boolean result = booking.cancel();

        // Assert
        assertTrue(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void testCancelFromCancelledFails() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.CANCELLED);

        // Act
        boolean result = booking.cancel();

        // Assert
        assertFalse(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void testCancelFromCompletedFails() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.COMPLETED);

        // Act
        boolean result = booking.cancel();

        // Assert
        assertFalse(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
    }

    @Test
    void testCompleteFromConfirmed() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.CONFIRMED);

        // Act
        boolean result = booking.complete();

        // Assert
        assertTrue(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
    }

    @Test
    void testCompleteFromPendingFails() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.PENDING);

        // Act
        boolean result = booking.complete();

        // Assert
        assertFalse(result);
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void testCompleteFromCompletedFails() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        booking.setStatus(BookingStatus.COMPLETED);

        // Act
        boolean result = booking.complete();

        // Assert
        assertFalse(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
    }

    @Test
    void testIsActive() {
        Booking booking = new Booking();

        booking.setStatus(BookingStatus.PENDING);
        assertTrue(booking.isActive());

        booking.setStatus(BookingStatus.CONFIRMED);
        assertTrue(booking.isActive());

        booking.setStatus(BookingStatus.CANCELLED);
        assertFalse(booking.isActive());

        booking.setStatus(BookingStatus.COMPLETED);
        assertFalse(booking.isActive());
    }

    @Test
    void testCanCancel() {
        Booking booking = new Booking();

        booking.setStatus(BookingStatus.PENDING);
        assertTrue(booking.canCancel());

        booking.setStatus(BookingStatus.CONFIRMED);
        assertTrue(booking.canCancel());

        booking.setStatus(BookingStatus.CANCELLED);
        assertFalse(booking.canCancel());

        booking.setStatus(BookingStatus.COMPLETED);
        assertFalse(booking.canCancel());
    }

    @Test
    void testGetUserId() {
        // Arrange
        Booking booking = new Booking();
        assertNull(booking.getUserId());

        User user = createTestUser();
        user.setId(42L);
        booking.setUser(user);

        // Act & Assert
        assertEquals(42L, booking.getUserId());
    }

    @Test
    void testGetSlotId() {
        // Arrange
        Booking booking = new Booking();
        assertNull(booking.getSlotId());

        AppointmentSlot slot = createTestSlot();
        slot.setId(99L);
        booking.setSlot(slot);

        // Act & Assert
        assertEquals(99L, booking.getSlotId());
    }

    @Test
    void testStatusTransitions() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        assertEquals(BookingStatus.PENDING, booking.getStatus());

        // PENDING -> CONFIRMED
        assertTrue(booking.confirm());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        // CONFIRMED -> COMPLETED
        assertTrue(booking.complete());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());

        // COMPLETED -> nothing allowed
        assertFalse(booking.cancel());
        assertFalse(booking.confirm());
        assertFalse(booking.complete());
    }

    @Test
    void testStatusTransitionsWithCancel() {
        // Arrange
        Booking booking = new Booking(createTestUser(), createTestSlot());
        assertEquals(BookingStatus.PENDING, booking.getStatus());

        // PENDING -> CANCELLED
        assertTrue(booking.cancel());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        // CANCELLED -> nothing allowed
        assertFalse(booking.confirm());
        assertFalse(booking.complete());
        assertFalse(booking.cancel());
    }

    @Test
    void testVersionField() {
        // Arrange
        Booking booking = new Booking();

        // Initial version
        assertEquals(0L, booking.getVersion());

        // Set version (simulating optimistic lock increment)
        booking.setVersion(1L);
        assertEquals(1L, booking.getVersion());

        booking.setVersion(5L);
        assertEquals(5L, booking.getVersion());
    }

    @Test
    void testRemarkField() {
        // Arrange
        Booking booking = new Booking();

        // Act & Assert
        assertNull(booking.getRemark());

        booking.setRemark("Special request");
        assertEquals("Special request", booking.getRemark());

        booking.setRemark(null);
        assertNull(booking.getRemark());

        booking.setRemark("");
        assertEquals("", booking.getRemark());
    }

    @Test
    void testLongRemark() {
        // Arrange
        Booking booking = new Booking();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is a long remark. ");
        }
        String longRemark = sb.toString();

        // Act
        booking.setRemark(longRemark);

        // Assert
        assertEquals(longRemark, booking.getRemark());
    }

    @Test
    void testUserAssociation() {
        // Arrange
        User user = createTestUser();
        user.setId(123L);
        Booking booking = new Booking(user, createTestSlot());

        // Assert
        assertNotNull(booking.getUser());
        assertEquals(123L, booking.getUser().getId());
        assertEquals("testuser", booking.getUser().getUsername());
    }

    @Test
    void testSlotAssociation() {
        // Arrange
        AppointmentSlot slot = createTestSlot();
        slot.setId(456L);
        Booking booking = new Booking(createTestUser(), slot);

        // Assert
        assertNotNull(booking.getSlot());
        assertEquals(456L, booking.getSlot().getId());
        assertEquals(LocalTime.of(9, 0), booking.getSlot().getStartTime());
    }
}
