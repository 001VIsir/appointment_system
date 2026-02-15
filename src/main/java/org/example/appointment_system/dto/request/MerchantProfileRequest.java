package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for merchant profile create/update request.
 *
 * <p>Contains all fields that can be set or modified on a merchant profile.</p>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>businessName: Required, 2-100 characters</li>
 *   <li>description: Optional, max 1000 characters</li>
 *   <li>phone: Optional, max 20 characters, valid phone format</li>
 *   <li>address: Optional, max 255 characters</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfileRequest {

    /**
     * The business name of the merchant.
     * Required field, displayed to customers.
     */
    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String businessName;

    /**
     * Optional description of the business.
     * Can be used to describe services offered.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Contact phone number.
     * Optional but recommended for customer contact.
     */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]*$", message = "Invalid phone number format")
    private String phone;

    /**
     * Business address.
     * Optional, useful for physical locations.
     */
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
}
