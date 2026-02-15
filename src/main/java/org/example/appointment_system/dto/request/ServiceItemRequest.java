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
import org.example.appointment_system.enums.ServiceCategory;

import java.math.BigDecimal;

/**
 * DTO for service item create/update request.
 *
 * <p>Contains all fields that can be set or modified on a service item.</p>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>name: Required, 2-100 characters</li>
 *   <li>description: Optional, max 1000 characters</li>
 *   <li>category: Required, must be a valid ServiceCategory</li>
 *   <li>duration: Required, minimum 5 minutes</li>
 *   <li>price: Required, minimum 0</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemRequest {

    /**
     * The name of the service.
     * Required field, displayed to customers.
     */
    @NotBlank(message = "Service name is required")
    @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters")
    private String name;

    /**
     * Optional description of the service.
     * Can be used to describe what the service includes.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Category of the service.
     * Used for classification and filtering.
     */
    @NotNull(message = "Service category is required")
    private ServiceCategory category;

    /**
     * Duration of the service in minutes.
     * Minimum 5 minutes to ensure meaningful appointments.
     */
    @NotNull(message = "Duration is required")
    @Min(value = 5, message = "Duration must be at least 5 minutes")
    private Integer duration;

    /**
     * Price of the service.
     * Can be 0 for free services.
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;

    /**
     * Whether the service is active and available for booking.
     * Defaults to true for new services.
     */
    private Boolean active;
}
