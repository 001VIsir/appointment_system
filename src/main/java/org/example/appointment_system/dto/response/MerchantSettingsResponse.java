package org.example.appointment_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for merchant settings response.
 *
 * <p>Contains all configurable settings for a merchant account
 * with their current values.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantSettingsResponse {

    /**
     * Session timeout in seconds.
     */
    private Integer sessionTimeout;

    /**
     * Enable email notifications for new bookings.
     */
    private Boolean notificationsEnabled;

    /**
     * Business timezone.
     */
    private String timezone;

    /**
     * Number of days in advance that bookings can be made.
     */
    private Integer bookingAdvanceDays;

    /**
     * Hours before appointment when cancellation is still allowed.
     */
    private Integer cancelDeadlineHours;

    /**
     * Enable automatic confirmation of bookings.
     */
    private Boolean autoConfirmBookings;

    /**
     * Maximum bookings per user per day.
     */
    private Integer maxBookingsPerUserPerDay;
}
