package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * DTO for time slot response data.
 *
 * <p>Contains slot information including availability status.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {

    /**
     * Unique identifier of the slot.
     */
    private Long id;

    /**
     * Start time of the slot.
     */
    private LocalTime startTime;

    /**
     * End time of the slot.
     */
    private LocalTime endTime;

    /**
     * Maximum capacity of the slot.
     */
    private Integer capacity;

    /**
     * Current number of bookings.
     */
    private Integer bookedCount;

    /**
     * Number of available spots.
     */
    private Integer availableCount;

    /**
     * Whether the slot has available capacity.
     */
    private Boolean hasCapacity;
}
