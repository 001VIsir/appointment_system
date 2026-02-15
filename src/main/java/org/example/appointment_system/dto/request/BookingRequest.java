package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for booking creation request.
 *
 * <p>Contains the required information for creating a new booking.</p>
 *
 * <h3>Validation rules:</h3>
 * <ul>
 *   <li>slotId: Required, must be a valid slot ID</li>
 *   <li>remark: Optional, max 500 characters</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    /**
     * ID of the appointment slot to book.
     * Required field.
     */
    private Long slotId;

    /**
     * Optional remark or note from the user.
     * Can contain special requests or notes for the merchant.
     * Max 500 characters.
     */
    @Size(max = 500, message = "Remark must be at most 500 characters")
    private String remark;
}
