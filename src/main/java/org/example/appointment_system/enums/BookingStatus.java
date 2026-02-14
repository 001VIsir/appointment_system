package org.example.appointment_system.enums;

/**
 * Booking status enumeration for tracking booking lifecycle.
 *
 * <p>Defines the possible states of a booking:</p>
 * <ul>
 *   <li>{@link #PENDING} - Booking created but not yet confirmed</li>
 *   <li>{@link #CONFIRMED} - Booking confirmed and ready</li>
 *   <li>{@link #CANCELLED} - Booking cancelled by user or system</li>
 *   <li>{@link #COMPLETED} - Booking completed (appointment finished)</li>
 * </ul>
 */
public enum BookingStatus {

    /**
     * Pending status - booking has been created but not yet confirmed.
     * This is the initial state when a user creates a booking.
     */
    PENDING("pending", "Pending"),

    /**
     * Confirmed status - booking has been confirmed and is ready.
     * The slot is reserved and the user should attend at the scheduled time.
     */
    CONFIRMED("confirmed", "Confirmed"),

    /**
     * Cancelled status - booking has been cancelled.
     * Can be cancelled by the user or by the system (e.g., no-show).
     */
    CANCELLED("cancelled", "Cancelled"),

    /**
     * Completed status - the appointment has been completed.
     * This is the final state after the appointment has finished.
     */
    COMPLETED("completed", "Completed");

    private final String code;
    private final String displayName;

    BookingStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Get the database code for this status.
     *
     * @return the status code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this booking can be cancelled.
     * Only PENDING and CONFIRMED bookings can be cancelled.
     *
     * @return true if the booking can be cancelled
     */
    public boolean canCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Check if this booking can be confirmed.
     * Only PENDING bookings can be confirmed.
     *
     * @return true if the booking can be confirmed
     */
    public boolean canConfirm() {
        return this == PENDING;
    }

    /**
     * Check if this booking can be marked as completed.
     * Only CONFIRMED bookings can be completed.
     *
     * @return true if the booking can be completed
     */
    public boolean canComplete() {
        return this == CONFIRMED;
    }

    /**
     * Check if this booking is in an active state.
     * Active bookings are PENDING or CONFIRMED.
     *
     * @return true if the booking is active
     */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Check if this booking is in a final state.
     * Final states are CANCELLED or COMPLETED.
     *
     * @return true if the booking is in a final state
     */
    public boolean isFinal() {
        return this == CANCELLED || this == COMPLETED;
    }
}
