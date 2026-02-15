package org.example.appointment_system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Response DTO for user statistics.
 *
 * <p>Contains various user metrics and statistics:</p>
 * <ul>
 *   <li>Total user counts by role</li>
 *   <li>New user registration statistics</li>
 *   <li>User activity statistics</li>
 * </ul>
 */
@Data
@Builder
public class UserStatsResponse {

    // ============================================
    // Overall User Statistics
    // ============================================

    /**
     * Total number of users.
     */
    private long totalUsers;

    /**
     * Total number of enabled users.
     */
    private long enabledUsers;

    /**
     * Total number of disabled users.
     */
    private long disabledUsers;

    // ============================================
    // Role-based Statistics
    // ============================================

    /**
     * Number of users with ADMIN role.
     */
    private long adminCount;

    /**
     * Number of users with MERCHANT role.
     */
    private long merchantCount;

    /**
     * Number of users with USER role.
     */
    private long userCount;

    // ============================================
    // Registration Statistics
    // ============================================

    /**
     * Number of new users registered today.
     */
    private long todayNewUsers;

    /**
     * Number of new users registered this week.
     */
    private long weekNewUsers;

    /**
     * Number of new users registered this month.
     */
    private long monthNewUsers;

    // ============================================
    // Activity Statistics
    // ============================================

    /**
     * Number of users who made at least one booking.
     */
    private long activeUsersWithBookings;

    /**
     * Number of merchants with at least one service item.
     */
    private long activeMerchantsWithServices;

    /**
     * Average bookings per user.
     */
    private double averageBookingsPerUser;
}
