package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for appointment task creation/update request.
 *
 * <p>Contains all fields needed to create or update an appointment task.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTaskRequest {

    /**
     * ID of the service item this task belongs to.
     * Required for task creation.
     */
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    /**
     * Title of the appointment task.
     * Required, 2-100 characters.
     */
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    /**
     * Optional description of the appointment task.
     * Maximum 1000 characters.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Date of the appointment task.
     * Required, must be today or in the future for new tasks.
     */
    @NotNull(message = "Task date is required")
    private LocalDate taskDate;

    /**
     * Maximum total bookings for this task.
     * Required, minimum 1.
     */
    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;

    /**
     * Whether the task is active.
     * Optional, defaults to true.
     */
    private Boolean active;
}
