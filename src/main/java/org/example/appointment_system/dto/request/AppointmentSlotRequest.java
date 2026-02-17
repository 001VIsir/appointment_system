package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * DTO for appointment slot creation request.
 *
 * <p>Contains all fields needed to create a time slot within a task.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSlotRequest {

    /**
     * Start time of the slot.
     * Required.
     */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /**
     * End time of the slot.
     * Required.
     */
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /**
     * Maximum bookings allowed for this slot.
     * Required, minimum 1.
     */
    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}
