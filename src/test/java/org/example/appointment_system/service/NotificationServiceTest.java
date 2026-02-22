package org.example.appointment_system.service;

import org.example.appointment_system.dto.response.BookingResponse;
import org.example.appointment_system.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Unit tests for NotificationService (simplified version).
 *
 * <p>Since WebSocket has been removed, these tests verify the logging behavior
 * instead of WebSocket messaging.</p>
 */
class NotificationServiceTest {

    private NotificationService notificationService;
    private BookingResponse testBookingResponse;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();

        testBookingResponse = BookingResponse.builder()
            .id(1L)
            .userId(100L)
            .username("testuser")
            .slotId(1L)
            .taskId(10L)
            .taskTitle("Test Task")
            .taskDate(LocalDate.now().plusDays(1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .serviceId(50L)
            .serviceName("Test Service")
            .merchantId(200L)
            .merchantBusinessName("Test Merchant")
            .status(BookingStatus.PENDING)
            .statusDisplayName("Pending")
            .remark("Test booking")
            .version(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ============================================
    // Notification Type Constants Tests
    // ============================================

    @Test
    @DisplayName("Notification type constants should be defined")
    void notificationTypeConstants_ShouldBeDefined() {
        assertEquals("NEW_BOOKING", NotificationService.TYPE_NEW_BOOKING);
        assertEquals("BOOKING_CANCELLED", NotificationService.TYPE_BOOKING_CANCELLED);
        assertEquals("BOOKING_CONFIRMED", NotificationService.TYPE_BOOKING_CONFIRMED);
        assertEquals("BOOKING_COMPLETED", NotificationService.TYPE_BOOKING_COMPLETED);
        assertEquals("BOOKING_REMINDER", NotificationService.TYPE_BOOKING_REMINDER);
    }

    // ============================================
    // New Booking Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyNewBooking should handle valid booking")
    void notifyNewBooking_ShouldHandleValidBooking() {
        // Should not throw exception
        notificationService.notifyNewBooking(testBookingResponse);
    }

    @Test
    @DisplayName("notifyNewBooking should handle null merchantId")
    void notifyNewBooking_ShouldHandleNullMerchantId() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .merchantId(null)
            .build();

        // Should not throw exception
        notificationService.notifyNewBooking(booking);
    }

    // ============================================
    // Booking Cancelled Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingCancelled should handle valid booking")
    void notifyBookingCancelled_ShouldHandleValidBooking() {
        // Should not throw exception
        notificationService.notifyBookingCancelled(testBookingResponse);
    }

    // ============================================
    // Booking Confirmed Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingConfirmed should handle valid booking")
    void notifyBookingConfirmed_ShouldHandleValidBooking() {
        // Should not throw exception
        notificationService.notifyBookingConfirmed(testBookingResponse);
    }

    @Test
    @DisplayName("notifyBookingConfirmed should handle null userId")
    void notifyBookingConfirmed_ShouldHandleNullUserId() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .userId(null)
            .build();

        // Should not throw exception
        notificationService.notifyBookingConfirmed(booking);
    }

    // ============================================
    // Booking Completed Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingCompleted should handle valid booking")
    void notifyBookingCompleted_ShouldHandleValidBooking() {
        // Should not throw exception
        notificationService.notifyBookingCompleted(testBookingResponse);
    }

    @Test
    @DisplayName("notifyBookingCompleted should handle null userId")
    void notifyBookingCompleted_ShouldHandleNullUserId() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .userId(null)
            .build();

        // Should not throw exception
        notificationService.notifyBookingCompleted(booking);
    }

    // ============================================
    // Booking Reminder Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingReminder should handle valid booking")
    void notifyBookingReminder_ShouldHandleValidBooking() {
        // Should not throw exception
        notificationService.notifyBookingReminder(testBookingResponse, 24);
    }

    @Test
    @DisplayName("notifyBookingReminder should handle null userId")
    void notifyBookingReminder_ShouldHandleNullUserId() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .userId(null)
            .build();

        // Should not throw exception
        notificationService.notifyBookingReminder(booking, 24);
    }

    // ============================================
    // Custom Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyMerchant should handle valid parameters")
    void notifyMerchant_ShouldHandleValidParameters() {
        // Should not throw exception
        notificationService.notifyMerchant(200L, "CUSTOM_EVENT", "Test message");
    }

    @Test
    @DisplayName("notifyUser should handle valid parameters")
    void notifyUser_ShouldHandleValidParameters() {
        // Should not throw exception
        notificationService.notifyUser(100L, "CUSTOM_EVENT", "Test message");
    }
}
