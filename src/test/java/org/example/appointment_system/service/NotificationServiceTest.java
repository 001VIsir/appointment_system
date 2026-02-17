package org.example.appointment_system.service;

import org.example.appointment_system.dto.response.BookingResponse;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.websocket.dto.BookingNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private BookingResponse testBookingResponse;

    @BeforeEach
    void setUp() {
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
    // New Booking Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyNewBooking - Should send notification to merchant topic")
    void notifyNewBooking_ShouldSendToMerchantTopic() {
        notificationService.notifyNewBooking(testBookingResponse);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/merchant/200"),
            any(BookingNotification.class)
        );
    }

    @Test
    @DisplayName("notifyNewBooking - Should include correct notification type")
    void notifyNewBooking_ShouldIncludeCorrectType() {
        ArgumentCaptor<BookingNotification> captor = ArgumentCaptor.forClass(BookingNotification.class);

        notificationService.notifyNewBooking(testBookingResponse);

        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        assertEquals(NotificationService.TYPE_NEW_BOOKING, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getBookingId());
        assertEquals("testuser", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("notifyNewBooking - Should not send when merchantId is null")
    void notifyNewBooking_ShouldNotSendWhenMerchantIdIsNull() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .merchantId(null)
            .build();

        notificationService.notifyNewBooking(booking);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(BookingNotification.class));
    }

    // ============================================
    // Booking Cancelled Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingCancelled - Should send to both merchant and user")
    void notifyBookingCancelled_ShouldSendToBothMerchantAndUser() {
        notificationService.notifyBookingCancelled(testBookingResponse);

        verify(messagingTemplate, times(2)).convertAndSend(any(String.class), any(BookingNotification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/merchant/200"), any(BookingNotification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/100"), any(BookingNotification.class));
    }

    @Test
    @DisplayName("notifyBookingCancelled - Should include correct type")
    void notifyBookingCancelled_ShouldIncludeCorrectType() {
        ArgumentCaptor<BookingNotification> captor = ArgumentCaptor.forClass(BookingNotification.class);

        notificationService.notifyBookingCancelled(testBookingResponse);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(any(String.class), captor.capture());
        assertEquals(NotificationService.TYPE_BOOKING_CANCELLED, captor.getValue().getType());
    }

    @Test
    @DisplayName("notifyBookingCancelled - Should send only to user when merchantId is null")
    void notifyBookingCancelled_ShouldSendOnlyToUserWhenMerchantIdIsNull() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .userId(100L)
            .merchantId(null)
            .status(BookingStatus.CANCELLED)
            .build();

        notificationService.notifyBookingCancelled(booking);

        verify(messagingTemplate, times(1)).convertAndSend(any(String.class), any(BookingNotification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/100"), any(BookingNotification.class));
    }

    // ============================================
    // Booking Confirmed Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingConfirmed - Should send to user topic")
    void notifyBookingConfirmed_ShouldSendToUserTopic() {
        notificationService.notifyBookingConfirmed(testBookingResponse);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/user/100"),
            any(BookingNotification.class)
        );
    }

    @Test
    @DisplayName("notifyBookingConfirmed - Should include correct type and message")
    void notifyBookingConfirmed_ShouldIncludeCorrectType() {
        ArgumentCaptor<BookingNotification> captor = ArgumentCaptor.forClass(BookingNotification.class);

        notificationService.notifyBookingConfirmed(testBookingResponse);

        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        assertEquals(NotificationService.TYPE_BOOKING_CONFIRMED, captor.getValue().getType());
        assertNotNull(captor.getValue().getMessage());
        assertTrue(captor.getValue().getMessage().contains("confirmed"));
    }

    @Test
    @DisplayName("notifyBookingConfirmed - Should not send when userId is null")
    void notifyBookingConfirmed_ShouldNotSendWhenUserIdIsNull() {
        BookingResponse booking = BookingResponse.builder()
            .id(1L)
            .userId(null)
            .build();

        notificationService.notifyBookingConfirmed(booking);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(BookingNotification.class));
    }

    // ============================================
    // Booking Completed Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingCompleted - Should send to user topic")
    void notifyBookingCompleted_ShouldSendToUserTopic() {
        notificationService.notifyBookingCompleted(testBookingResponse);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/user/100"),
            any(BookingNotification.class)
        );
    }

    @Test
    @DisplayName("notifyBookingCompleted - Should include correct type")
    void notifyBookingCompleted_ShouldIncludeCorrectType() {
        ArgumentCaptor<BookingNotification> captor = ArgumentCaptor.forClass(BookingNotification.class);

        notificationService.notifyBookingCompleted(testBookingResponse);

        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        assertEquals(NotificationService.TYPE_BOOKING_COMPLETED, captor.getValue().getType());
        assertTrue(captor.getValue().getMessage().contains("completed"));
    }

    // ============================================
    // Booking Reminder Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyBookingReminder - Should send reminder with hours info")
    void notifyBookingReminder_ShouldSendReminderWithHoursInfo() {
        notificationService.notifyBookingReminder(testBookingResponse, 24);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/user/100"),
            any(BookingNotification.class)
        );
    }

    @Test
    @DisplayName("notifyBookingReminder - Should include hours in message")
    void notifyBookingReminder_ShouldIncludeHoursInMessage() {
        ArgumentCaptor<BookingNotification> captor = ArgumentCaptor.forClass(BookingNotification.class);

        notificationService.notifyBookingReminder(testBookingResponse, 24);

        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        assertEquals(NotificationService.TYPE_BOOKING_REMINDER, captor.getValue().getType());
        assertTrue(captor.getValue().getMessage().contains("24 hours"));
    }

    // ============================================
    // Custom Notification Tests
    // ============================================

    @Test
    @DisplayName("notifyMerchant - Should send custom notification to merchant")
    void notifyMerchant_ShouldSendCustomNotification() {
        notificationService.notifyMerchant(200L, "CUSTOM_EVENT", "Test message");

        verify(messagingTemplate).convertAndSend(
            eq("/topic/merchant/200"),
            any(BookingNotification.class)
        );
    }

    @Test
    @DisplayName("notifyUser - Should send custom notification to user")
    void notifyUser_ShouldSendCustomNotification() {
        notificationService.notifyUser(100L, "CUSTOM_EVENT", "Test message");

        verify(messagingTemplate).convertAndSend(
            eq("/topic/user/100"),
            any(BookingNotification.class)
        );
    }

    // ============================================
    // Notification Content Tests
    // ============================================

    @Test
    @DisplayName("Notification should include all booking details")
    void notification_ShouldIncludeAllBookingDetails() {
        ArgumentCaptor<BookingNotification> captor = ArgumentCaptor.forClass(BookingNotification.class);

        notificationService.notifyNewBooking(testBookingResponse);

        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());
        BookingNotification notification = captor.getValue();

        assertEquals(1L, notification.getBookingId());
        assertEquals(100L, notification.getUserId());
        assertEquals("testuser", notification.getUsername());
        assertEquals("Test Service", notification.getServiceName());
        assertEquals(LocalDate.now().plusDays(1), notification.getTaskDate());
        assertEquals(LocalTime.of(10, 0), notification.getStartTime());
        assertEquals(LocalTime.of(11, 0), notification.getEndTime());
        assertEquals("PENDING", notification.getStatus());
        assertNotNull(notification.getTimestamp());
        assertNotNull(notification.getMessage());
    }
}
