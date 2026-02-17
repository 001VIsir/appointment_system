package org.example.appointment_system.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for booking-related WebSocket notifications.
 *
 * <p>Sent to merchants and users when booking events occur.</p>
 *
 * <h3>Notification Types:</h3>
 * <ul>
 *   <li>NEW_BOOKING - New booking created</li>
 *   <li>BOOKING_CANCELLED - Booking was cancelled</li>
 *   <li>BOOKING_CONFIRMED - Booking was confirmed by merchant</li>
 *   <li>BOOKING_COMPLETED - Booking was marked as completed</li>
 *   <li>BOOKING_REMINDER - Reminder for upcoming appointment</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotification {

    /**
     * Type of notification.
     */
    private String type;

    /**
     * ID of the related booking.
     */
    private Long bookingId;

    /**
     * ID of the user who made the booking.
     */
    private Long userId;

    /**
     * Username of the user (for merchant notifications).
     */
    private String username;

    /**
     * Business name of the merchant (for user notifications).
     */
    private String merchantBusinessName;

    /**
     * Name of the service.
     */
    private String serviceName;

    /**
     * Date of the appointment.
     */
    private LocalDate taskDate;

    /**
     * Start time of the appointment.
     */
    private LocalTime startTime;

    /**
     * End time of the appointment.
     */
    private LocalTime endTime;

    /**
     * Current status of the booking.
     */
    private String status;

    /**
     * Human-readable message describing the notification.
     */
    private String message;

    /**
     * Timestamp when the notification was created.
     */
    private LocalDateTime timestamp;
}
