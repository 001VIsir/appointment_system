package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for booking response data.
 *
 * <p>Contains all booking information for API responses,
 * including related task and service details.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    /**
     * Unique identifier of the booking.
     */
    private Long id;

    /**
     * ID of the user who made the booking.
     */
    private Long userId;

    /**
     * Username of the user who made the booking.
     */
    private String username;

    /**
     * ID of the appointment slot.
     */
    private Long slotId;

    /**
     * ID of the appointment task.
     */
    private Long taskId;

    /**
     * Title of the appointment task.
     */
    private String taskTitle;

    /**
     * Date of the appointment.
     */
    private LocalDate taskDate;

    /**
     * Start time of the booked slot.
     */
    private LocalTime startTime;

    /**
     * End time of the booked slot.
     */
    private LocalTime endTime;

    /**
     * ID of the service item.
     */
    private Long serviceId;

    /**
     * Name of the service.
     */
    private String serviceName;

    /**
     * ID of the merchant.
     */
    private Long merchantId;

    /**
     * Business name of the merchant.
     */
    private String merchantBusinessName;

    /**
     * Current status of the booking.
     */
    private BookingStatus status;

    /**
     * Display name of the status.
     */
    private String statusDisplayName;

    /**
     * Optional remark from the user.
     */
    private String remark;

    /**
     * Optimistic lock version number.
     */
    private Long version;

    /**
     * Timestamp when the booking was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the booking was last updated.
     */
    private LocalDateTime updatedAt;
}
