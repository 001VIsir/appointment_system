package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for appointment task response data.
 *
 * <p>Contains complete task information including related service details.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTaskResponse {

    /**
     * Unique identifier of the task.
     */
    private Long id;

    /**
     * ID of the associated service item.
     */
    private Long serviceId;

    /**
     * Name of the associated service.
     */
    private String serviceName;

    /**
     * ID of the merchant who owns this task.
     */
    private Long merchantId;

    /**
     * Business name of the merchant.
     */
    private String merchantBusinessName;

    /**
     * Title of the task.
     */
    private String title;

    /**
     * Description of the task.
     */
    private String description;

    /**
     * Date of the task.
     */
    private LocalDate taskDate;

    /**
     * Maximum total bookings for this task.
     */
    private Integer totalCapacity;

    /**
     * Total number of slots in this task.
     */
    private Integer slotCount;

    /**
     * Total capacity across all slots.
     */
    private Integer totalSlotCapacity;

    /**
     * Total bookings made across all slots.
     */
    private Integer totalBookedCount;

    /**
     * Whether the task is active.
     */
    private Boolean active;

    /**
     * Timestamp when the task was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the task was last updated.
     */
    private LocalDateTime updatedAt;
}
