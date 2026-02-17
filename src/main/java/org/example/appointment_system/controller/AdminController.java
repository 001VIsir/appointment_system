package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.response.BookingStatsResponse;
import org.example.appointment_system.dto.response.SystemStatsResponse;
import org.example.appointment_system.dto.response.UserStatsResponse;
import org.example.appointment_system.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST Controller for admin statistics and metrics operations.
 *
 * <p>Provides endpoints for system-wide statistics and monitoring.
 * All endpoints require ADMIN role.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>GET /api/admin/metrics - Get system metrics (API calls, errors, response times)</li>
 *   <li>GET /api/admin/stats/bookings - Get booking statistics</li>
 *   <li>GET /api/admin/stats/users - Get user statistics</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>All endpoints are protected by Spring Security and require
 * ADMIN role as configured in SecurityConfig.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Endpoints for system administration and statistics")
public class AdminController {

    private final StatisticsService statisticsService;

    /**
     * Get system metrics including API calls, errors, and response times.
     *
     * <p>This endpoint provides real-time system performance metrics
     * collected from Redis counters.</p>
     *
     * @return SystemStatsResponse with system metrics
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get system metrics",
        description = "Retrieves real-time system metrics including API call counts, " +
                      "error rates, response times, and resource usage. " +
                      "Requires ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "System metrics retrieved successfully",
                content = @Content(schema = @Schema(implementation = SystemStatsResponse.class))
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Access denied - ADMIN role required"
            )
        }
    )
    public ResponseEntity<SystemStatsResponse> getMetrics() {
        log.debug("Admin requesting system metrics");
        SystemStatsResponse response = statisticsService.getSystemStats();
        return ResponseEntity.ok(response);
    }

    /**
     * Get booking statistics for the entire system.
     *
     * <p>Optionally filter by date range using startDate and endDate parameters.</p>
     *
     * @param startDate optional start date for the statistics period (ISO format: yyyy-MM-dd)
     * @param endDate   optional end date for the statistics period (ISO format: yyyy-MM-dd)
     * @return BookingStatsResponse with booking statistics
     */
    @GetMapping("/stats/bookings")
    @Operation(
        summary = "Get booking statistics",
        description = "Retrieves booking statistics for the entire system. " +
                      "Optionally filter by date range using startDate and endDate parameters. " +
                      "Requires ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Booking statistics retrieved successfully",
                content = @Content(schema = @Schema(implementation = BookingStatsResponse.class))
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Access denied - ADMIN role required"
            )
        }
    )
    public ResponseEntity<BookingStatsResponse> getBookingStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("Admin requesting booking stats from {} to {}", startDate, endDate);
        BookingStatsResponse response = statisticsService.getBookingStats(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user statistics for the entire system.
     *
     * <p>Includes user counts by role, registration statistics, and activity metrics.</p>
     *
     * @return UserStatsResponse with user statistics
     */
    @GetMapping("/stats/users")
    @Operation(
        summary = "Get user statistics",
        description = "Retrieves user statistics including counts by role, " +
                      "registration statistics, and activity metrics. " +
                      "Requires ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "User statistics retrieved successfully",
                content = @Content(schema = @Schema(implementation = UserStatsResponse.class))
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Access denied - ADMIN role required"
            )
        }
    )
    public ResponseEntity<UserStatsResponse> getUserStats() {
        log.debug("Admin requesting user stats");
        UserStatsResponse response = statisticsService.getUserStats();
        return ResponseEntity.ok(response);
    }

    /**
     * Get combined dashboard statistics.
     *
     * <p>Returns a summary of all key metrics for admin dashboard.</p>
     *
     * @return DashboardStatsResponse with combined statistics
     */
    @GetMapping("/stats/dashboard")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Retrieves combined statistics for admin dashboard including " +
                      "booking stats, user stats, and system metrics. " +
                      "Requires ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Dashboard statistics retrieved successfully"
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Access denied - ADMIN role required"
            )
        }
    )
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        log.debug("Admin requesting dashboard stats");

        BookingStatsResponse bookingStats = statisticsService.getBookingStats();
        UserStatsResponse userStats = statisticsService.getUserStats();
        SystemStatsResponse systemStats = statisticsService.getSystemStats();

        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .bookings(bookingStats)
                .users(userStats)
                .system(systemStats)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Combined dashboard statistics response.
     */
    @lombok.Data
    @lombok.Builder
    @Schema(description = "Combined dashboard statistics response")
    public static class DashboardStatsResponse {
        @Schema(description = "Booking statistics")
        private BookingStatsResponse bookings;

        @Schema(description = "User statistics")
        private UserStatsResponse users;

        @Schema(description = "System metrics")
        private SystemStatsResponse system;
    }
}
