package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for merchant profile response data.
 *
 * <p>Contains all merchant profile information including settings.
 * Used for API responses containing merchant profile data.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfileResponse {

    /**
     * Unique identifier of the merchant profile.
     */
    private Long id;

    /**
     * ID of the associated user account.
     */
    private Long userId;

    /**
     * Username of the associated user account.
     */
    private String username;

    /**
     * The business name of the merchant.
     */
    private String businessName;

    /**
     * Optional description of the business.
     */
    private String description;

    /**
     * Contact phone number.
     */
    private String phone;

    /**
     * Business address.
     */
    private String address;

    /**
     * Merchant settings stored as JSON string.
     */
    private String settings;

    /**
     * Timestamp when the profile was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the profile was last updated.
     */
    private LocalDateTime updatedAt;
}
