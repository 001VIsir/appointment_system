package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for service item response data.
 *
 * <p>Contains all service item information for API responses.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemResponse {

    /**
     * Unique identifier of the service item.
     */
    private Long id;

    /**
     * ID of the merchant profile that owns this service.
     */
    private Long merchantId;

    /**
     * Business name of the merchant.
     */
    private String merchantBusinessName;

    /**
     * The name of the service.
     */
    private String name;

    /**
     * Optional description of the service.
     */
    private String description;

    /**
     * Category of the service.
     */
    private ServiceCategory category;

    /**
     * Category display name for frontend use.
     */
    private String categoryDisplayName;

    /**
     * Duration of the service in minutes.
     */
    private Integer duration;

    /**
     * Price of the service.
     */
    private BigDecimal price;

    /**
     * Whether the service is active and available for booking.
     */
    private Boolean active;

    /**
     * Timestamp when the service was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the service was last updated.
     */
    private LocalDateTime updatedAt;
}
