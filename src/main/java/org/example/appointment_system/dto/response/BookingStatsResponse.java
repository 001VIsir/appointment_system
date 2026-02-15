package org.example.appointment_system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * Response DTO for booking statistics.
 *
 * <p>Contains various booking metrics and statistics:</p>
 * <ul>
 *   <li>Total booking counts by status</li>
 *   <li>Today's booking statistics</li>
 *   <li>Date range statistics</li>
 *   <li>Merchant-specific statistics</li>
 * </ul>
 */
@Data
@Builder
public class BookingStatsResponse {

    // ============================================
    // Overall Statistics
    // ============================================

    /**
     * Total number of bookings (all time).
     */
    private long totalBookings;

    /**
     * Total number of active bookings (PENDING + CONFIRMED).
     */
    private long activeBookings;

    /**
     * Total number of pending bookings.
     */
    private long pendingBookings;

    /**
     * Total number of confirmed bookings.
     */
    private long confirmedBookings;

    /**
     * Total number of cancelled bookings.
     */
    private long cancelledBookings;

    /**
     * Total number of completed bookings.
     */
    private long completedBookings;

    // ============================================
    // Today's Statistics
    // ============================================

    /**
     * Number of bookings created today.
     */
    private long todayBookings;

    /**
     * Number of active bookings for today.
     */
    private long todayActiveBookings;

    /**
     * Number of completed bookings today.
     */
    private long todayCompletedBookings;

    // ============================================
    // Date Range Statistics
    // ============================================

    /**
     * Start date for the statistics period.
     */
    private LocalDate startDate;

    /**
     * End date for the statistics period.
     */
    private LocalDate endDate;

    /**
     * Number of bookings in the date range.
     */
    private long periodBookings;

    /**
     * Daily breakdown of bookings (date -> count).
     */
    private Map<LocalDate, Long> dailyBookings;

    // ============================================
    // Success Metrics
    // ============================================

    /**
     * Booking completion rate (completed / total).
     */
    private double completionRate;

    /**
     * Booking cancellation rate (cancelled / total).
     */
    private double cancellationRate;

    /**
     * Booking confirmation rate (confirmed / total).
     */
    private double confirmationRate;
}
