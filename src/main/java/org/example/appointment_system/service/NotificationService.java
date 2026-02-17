package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.response.BookingResponse;
import org.example.appointment_system.websocket.dto.BookingNotification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for sending real-time WebSocket notifications.
 *
 * <p>This service handles sending notifications to merchants and users
 * about booking-related events via WebSocket.</p>
 *
 * <h3>Notification Types:</h3>
 * <ul>
 *   <li>NEW_BOOKING - Sent to merchant when a new booking is created</li>
 *   <li>BOOKING_CANCELLED - Sent when a booking is cancelled</li>
 *   <li>BOOKING_CONFIRMED - Sent to user when booking is confirmed</li>
 *   <li>BOOKING_COMPLETED - Sent to user when booking is completed</li>
 *   <li>BOOKING_REMINDER - Sent to user before appointment</li>
 * </ul>
 *
 * <h3>Destination Patterns:</h3>
 * <ul>
 *   <li>/topic/merchant/{merchantId} - Merchant-specific notifications</li>
 *   <li>/topic/user/{userId} - User-specific notifications</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Notification type constants
    public static final String TYPE_NEW_BOOKING = "NEW_BOOKING";
    public static final String TYPE_BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String TYPE_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String TYPE_BOOKING_COMPLETED = "BOOKING_COMPLETED";
    public static final String TYPE_BOOKING_REMINDER = "BOOKING_REMINDER";

    /**
     * Notify merchant about a new booking.
     *
     * <p>Sends a notification to the merchant's WebSocket topic
     * when a user creates a new booking.</p>
     *
     * @param booking the booking response
     */
    public void notifyNewBooking(BookingResponse booking) {
        if (booking.getMerchantId() == null) {
            log.warn("Cannot send new booking notification - merchantId is null");
            return;
        }

        BookingNotification notification = BookingNotification.builder()
            .type(TYPE_NEW_BOOKING)
            .bookingId(booking.getId())
            .userId(booking.getUserId())
            .username(booking.getUsername())
            .serviceName(booking.getServiceName())
            .taskDate(booking.getTaskDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus().name())
            .timestamp(LocalDateTime.now())
            .message(String.format("New booking from %s for %s on %s at %s",
                booking.getUsername(),
                booking.getServiceName(),
                booking.getTaskDate(),
                booking.getStartTime()))
            .build();

        String destination = "/topic/merchant/" + booking.getMerchantId();
        messagingTemplate.convertAndSend(destination, notification);

        log.info("Sent NEW_BOOKING notification to {}: bookingId={}",
            destination, booking.getId());
    }

    /**
     * Notify about a booking cancellation.
     *
     * <p>Sends notifications to both the merchant and the user
     * when a booking is cancelled.</p>
     *
     * @param booking the booking response
     */
    public void notifyBookingCancelled(BookingResponse booking) {
        BookingNotification notification = BookingNotification.builder()
            .type(TYPE_BOOKING_CANCELLED)
            .bookingId(booking.getId())
            .userId(booking.getUserId())
            .username(booking.getUsername())
            .serviceName(booking.getServiceName())
            .taskDate(booking.getTaskDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus().name())
            .timestamp(LocalDateTime.now())
            .message(String.format("Booking #%d has been cancelled", booking.getId()))
            .build();

        // Notify merchant if available
        if (booking.getMerchantId() != null) {
            String merchantDestination = "/topic/merchant/" + booking.getMerchantId();
            messagingTemplate.convertAndSend(merchantDestination, notification);
            log.debug("Sent BOOKING_CANCELLED notification to merchant: {}", merchantDestination);
        }

        // Notify user
        if (booking.getUserId() != null) {
            String userDestination = "/topic/user/" + booking.getUserId();
            messagingTemplate.convertAndSend(userDestination, notification);
            log.debug("Sent BOOKING_CANCELLED notification to user: {}", userDestination);
        }

        log.info("Sent BOOKING_CANCELLED notifications for bookingId={}", booking.getId());
    }

    /**
     * Notify user about booking confirmation.
     *
     * @param booking the booking response
     */
    public void notifyBookingConfirmed(BookingResponse booking) {
        if (booking.getUserId() == null) {
            log.warn("Cannot send confirmation notification - userId is null");
            return;
        }

        BookingNotification notification = BookingNotification.builder()
            .type(TYPE_BOOKING_CONFIRMED)
            .bookingId(booking.getId())
            .merchantBusinessName(booking.getMerchantBusinessName())
            .serviceName(booking.getServiceName())
            .taskDate(booking.getTaskDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus().name())
            .timestamp(LocalDateTime.now())
            .message(String.format("Your booking for %s on %s at %s has been confirmed",
                booking.getServiceName(),
                booking.getTaskDate(),
                booking.getStartTime()))
            .build();

        String destination = "/topic/user/" + booking.getUserId();
        messagingTemplate.convertAndSend(destination, notification);

        log.info("Sent BOOKING_CONFIRMED notification to {}: bookingId={}",
            destination, booking.getId());
    }

    /**
     * Notify user about booking completion.
     *
     * @param booking the booking response
     */
    public void notifyBookingCompleted(BookingResponse booking) {
        if (booking.getUserId() == null) {
            log.warn("Cannot send completion notification - userId is null");
            return;
        }

        BookingNotification notification = BookingNotification.builder()
            .type(TYPE_BOOKING_COMPLETED)
            .bookingId(booking.getId())
            .merchantBusinessName(booking.getMerchantBusinessName())
            .serviceName(booking.getServiceName())
            .taskDate(booking.getTaskDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus().name())
            .timestamp(LocalDateTime.now())
            .message(String.format("Your booking for %s has been completed. Thank you!",
                booking.getServiceName()))
            .build();

        String destination = "/topic/user/" + booking.getUserId();
        messagingTemplate.convertAndSend(destination, notification);

        log.info("Sent BOOKING_COMPLETED notification to {}: bookingId={}",
            destination, booking.getId());
    }

    /**
     * Send a booking reminder to a user.
     *
     * <p>Typically called by the scheduled task to remind users
     * about upcoming appointments.</p>
     *
     * @param booking the booking response
     * @param hoursUntilAppointment hours until the appointment
     */
    public void notifyBookingReminder(BookingResponse booking, int hoursUntilAppointment) {
        if (booking.getUserId() == null) {
            log.warn("Cannot send reminder notification - userId is null");
            return;
        }

        BookingNotification notification = BookingNotification.builder()
            .type(TYPE_BOOKING_REMINDER)
            .bookingId(booking.getId())
            .merchantBusinessName(booking.getMerchantBusinessName())
            .serviceName(booking.getServiceName())
            .taskDate(booking.getTaskDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus().name())
            .timestamp(LocalDateTime.now())
            .message(String.format("Reminder: Your appointment for %s is in %d hours on %s at %s",
                booking.getServiceName(),
                hoursUntilAppointment,
                booking.getTaskDate(),
                booking.getStartTime()))
            .build();

        String destination = "/topic/user/" + booking.getUserId();
        messagingTemplate.convertAndSend(destination, notification);

        log.info("Sent BOOKING_REMINDER notification to {}: bookingId={}, hoursUntil={}",
            destination, booking.getId(), hoursUntilAppointment);
    }

    /**
     * Send a custom notification to a specific merchant.
     *
     * @param merchantId the merchant ID
     * @param type the notification type
     * @param message the message content
     */
    public void notifyMerchant(Long merchantId, String type, String message) {
        BookingNotification notification = BookingNotification.builder()
            .type(type)
            .timestamp(LocalDateTime.now())
            .message(message)
            .build();

        String destination = "/topic/merchant/" + merchantId;
        messagingTemplate.convertAndSend(destination, notification);

        log.info("Sent {} notification to merchant {}: {}", type, merchantId, message);
    }

    /**
     * Send a custom notification to a specific user.
     *
     * @param userId the user ID
     * @param type the notification type
     * @param message the message content
     */
    public void notifyUser(Long userId, String type, String message) {
        BookingNotification notification = BookingNotification.builder()
            .type(type)
            .timestamp(LocalDateTime.now())
            .message(message)
            .build();

        String destination = "/topic/user/" + userId;
        messagingTemplate.convertAndSend(destination, notification);

        log.info("Sent {} notification to user {}: {}", type, userId, message);
    }
}
