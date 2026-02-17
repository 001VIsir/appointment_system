package org.example.appointment_system.controller;

import org.example.appointment_system.dto.response.BookingStatsResponse;
import org.example.appointment_system.dto.response.SystemStatsResponse;
import org.example.appointment_system.dto.response.UserStatsResponse;
import org.example.appointment_system.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminController.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    @DisplayName("GET /api/admin/metrics - should return system metrics")
    void getMetrics_shouldReturnSystemStats() throws Exception {
        // Given
        SystemStatsResponse response = SystemStatsResponse.builder()
                .todayApiCalls(1000L)
                .lastHourApiCalls(100L)
                .apiCallsPerMinute(1.67)
                .todayErrors(10L)
                .lastHourErrors(1L)
                .errorRate(1.0)
                .clientErrors(8L)
                .serverErrors(2L)
                .averageResponseTimeMs(50.5)
                .maxResponseTimeMs(500L)
                .minResponseTimeMs(10L)
                .p95ResponseTimeMs(200L)
                .activeSessions(50L)
                .heapUsedMb(256L)
                .heapMaxMb(1024L)
                .uptimeSeconds(86400L)
                .build();

        when(statisticsService.getSystemStats()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayApiCalls").value(1000))
                .andExpect(jsonPath("$.lastHourApiCalls").value(100))
                .andExpect(jsonPath("$.apiCallsPerMinute").value(1.67))
                .andExpect(jsonPath("$.todayErrors").value(10))
                .andExpect(jsonPath("$.errorRate").value(1.0))
                .andExpect(jsonPath("$.clientErrors").value(8))
                .andExpect(jsonPath("$.serverErrors").value(2))
                .andExpect(jsonPath("$.averageResponseTimeMs").value(50.5))
                .andExpect(jsonPath("$.maxResponseTimeMs").value(500))
                .andExpect(jsonPath("$.minResponseTimeMs").value(10))
                .andExpect(jsonPath("$.p95ResponseTimeMs").value(200))
                .andExpect(jsonPath("$.activeSessions").value(50))
                .andExpect(jsonPath("$.heapUsedMb").value(256))
                .andExpect(jsonPath("$.heapMaxMb").value(1024))
                .andExpect(jsonPath("$.uptimeSeconds").value(86400));

        verify(statisticsService).getSystemStats();
    }

    @Test
    @DisplayName("GET /api/admin/stats/bookings - should return booking stats without date range")
    void getBookingStats_withoutDateRange_shouldReturnStats() throws Exception {
        // Given
        BookingStatsResponse response = BookingStatsResponse.builder()
                .totalBookings(100L)
                .activeBookings(20L)
                .pendingBookings(10L)
                .confirmedBookings(10L)
                .cancelledBookings(30L)
                .completedBookings(50L)
                .todayBookings(5L)
                .todayActiveBookings(3L)
                .todayCompletedBookings(2L)
                .completionRate(50.0)
                .cancellationRate(30.0)
                .confirmationRate(10.0)
                .build();

        when(statisticsService.getBookingStats(null, null)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/admin/stats/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(100))
                .andExpect(jsonPath("$.activeBookings").value(20))
                .andExpect(jsonPath("$.pendingBookings").value(10))
                .andExpect(jsonPath("$.confirmedBookings").value(10))
                .andExpect(jsonPath("$.cancelledBookings").value(30))
                .andExpect(jsonPath("$.completedBookings").value(50))
                .andExpect(jsonPath("$.todayBookings").value(5))
                .andExpect(jsonPath("$.completionRate").value(50.0))
                .andExpect(jsonPath("$.cancellationRate").value(30.0));

        verify(statisticsService).getBookingStats(null, null);
    }

    @Test
    @DisplayName("GET /api/admin/stats/bookings - should return booking stats with date range")
    void getBookingStats_withDateRange_shouldReturnStats() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 15);

        BookingStatsResponse response = BookingStatsResponse.builder()
                .totalBookings(100L)
                .activeBookings(20L)
                .startDate(startDate)
                .endDate(endDate)
                .periodBookings(50L)
                .dailyBookings(Map.of(LocalDate.of(2026, 2, 10), 5L))
                .build();

        when(statisticsService.getBookingStats(startDate, endDate)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/admin/stats/bookings")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(100))
                .andExpect(jsonPath("$.periodBookings").value(50));

        verify(statisticsService).getBookingStats(startDate, endDate);
    }

    @Test
    @DisplayName("GET /api/admin/stats/users - should return user stats")
    void getUserStats_shouldReturnUserStats() throws Exception {
        // Given
        UserStatsResponse response = UserStatsResponse.builder()
                .totalUsers(1000L)
                .enabledUsers(950L)
                .disabledUsers(50L)
                .adminCount(5L)
                .merchantCount(100L)
                .userCount(895L)
                .todayNewUsers(10L)
                .weekNewUsers(50L)
                .monthNewUsers(200L)
                .activeUsersWithBookings(500L)
                .activeMerchantsWithServices(80L)
                .averageBookingsPerUser(2.5)
                .build();

        when(statisticsService.getUserStats()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/admin/stats/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1000))
                .andExpect(jsonPath("$.enabledUsers").value(950))
                .andExpect(jsonPath("$.disabledUsers").value(50))
                .andExpect(jsonPath("$.adminCount").value(5))
                .andExpect(jsonPath("$.merchantCount").value(100))
                .andExpect(jsonPath("$.userCount").value(895))
                .andExpect(jsonPath("$.todayNewUsers").value(10))
                .andExpect(jsonPath("$.weekNewUsers").value(50))
                .andExpect(jsonPath("$.monthNewUsers").value(200))
                .andExpect(jsonPath("$.activeUsersWithBookings").value(500))
                .andExpect(jsonPath("$.activeMerchantsWithServices").value(80))
                .andExpect(jsonPath("$.averageBookingsPerUser").value(2.5));

        verify(statisticsService).getUserStats();
    }

    @Test
    @DisplayName("GET /api/admin/stats/dashboard - should return combined dashboard stats")
    void getDashboardStats_shouldReturnCombinedStats() throws Exception {
        // Given
        BookingStatsResponse bookingStats = BookingStatsResponse.builder()
                .totalBookings(100L)
                .build();

        UserStatsResponse userStats = UserStatsResponse.builder()
                .totalUsers(1000L)
                .build();

        SystemStatsResponse systemStats = SystemStatsResponse.builder()
                .todayApiCalls(500L)
                .build();

        when(statisticsService.getBookingStats()).thenReturn(bookingStats);
        when(statisticsService.getUserStats()).thenReturn(userStats);
        when(statisticsService.getSystemStats()).thenReturn(systemStats);

        // When & Then
        mockMvc.perform(get("/api/admin/stats/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings.totalBookings").value(100))
                .andExpect(jsonPath("$.users.totalUsers").value(1000))
                .andExpect(jsonPath("$.system.todayApiCalls").value(500));

        verify(statisticsService).getBookingStats();
        verify(statisticsService).getUserStats();
        verify(statisticsService).getSystemStats();
    }

    @Test
    @DisplayName("GET /api/admin/metrics - should handle empty stats")
    void getMetrics_withEmptyStats_shouldReturnZeros() throws Exception {
        // Given
        SystemStatsResponse response = SystemStatsResponse.builder()
                .todayApiCalls(0L)
                .lastHourApiCalls(0L)
                .apiCallsPerMinute(0.0)
                .todayErrors(0L)
                .errorRate(0.0)
                .averageResponseTimeMs(0.0)
                .activeSessions(0L)
                .build();

        when(statisticsService.getSystemStats()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayApiCalls").value(0))
                .andExpect(jsonPath("$.lastHourApiCalls").value(0))
                .andExpect(jsonPath("$.errorRate").value(0.0));

        verify(statisticsService).getSystemStats();
    }
}
