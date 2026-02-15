package org.example.appointment_system.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for merchant settings update request.
 *
 * <p>Contains configurable settings for a merchant account.
 * Settings are stored as JSON in the database for flexibility.</p>
 *
 * <h3>Available Settings:</h3>
 * <ul>
 *   <li>sessionTimeout: Session timeout in seconds (default: 14400 = 4 hours)</li>
 *   <li>notifications: Enable/disable email notifications</li>
 *   <li>timezone: Business timezone (e.g., "Asia/Shanghai")</li>
 *   <li>bookingAdvanceDays: How many days in advance bookings can be made</li>
 *   <li>cancelDeadline: Hours before appointment when cancellation is allowed</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantSettingsRequest {

    /**
     * Session timeout in seconds.
     * Default: 14400 (4 hours).
     * Min: 1800 (30 minutes), Max: 86400 (24 hours).
     */
    private Integer sessionTimeout;

    /**
     * Enable email notifications for new bookings.
     */
    private Boolean notificationsEnabled;

    /**
     * Business timezone for displaying times to customers.
     * Example: "Asia/Shanghai", "America/New_York".
     */
    private String timezone;

    /**
     * Number of days in advance that bookings can be made.
     * Default: 30 days.
     */
    private Integer bookingAdvanceDays;

    /**
     * Hours before appointment when cancellation is still allowed.
     * Default: 24 hours.
     */
    private Integer cancelDeadlineHours;

    /**
     * Enable automatic confirmation of bookings.
     * If true, bookings are automatically confirmed instead of pending.
     */
    private Boolean autoConfirmBookings;

    /**
     * Maximum bookings per user per day.
     * 0 or null means unlimited.
     */
    private Integer maxBookingsPerUserPerDay;
}
