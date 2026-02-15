package org.example.appointment_system.service;

import org.example.appointment_system.dto.response.BookingStatsResponse;
import org.example.appointment_system.dto.response.SystemStatsResponse;
import org.example.appointment_system.dto.response.UserStatsResponse;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.HashOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StatisticsService.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private StatisticsService statisticsService;

    private User testUser;
    private Booking testBooking1;
    private Booking testBooking2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);

        testBooking1 = new Booking();
        testBooking1.setId(1L);
        testBooking1.setUser(testUser);
        testBooking1.setStatus(BookingStatus.CONFIRMED);
        testBooking1.setCreatedAt(LocalDateTime.now().minusHours(2));
        testBooking1.setUpdatedAt(LocalDateTime.now());

        testBooking2 = new Booking();
        testBooking2.setId(2L);
        testBooking2.setUser(testUser);
        testBooking2.setStatus(BookingStatus.PENDING);
        testBooking2.setCreatedAt(LocalDateTime.now().minusHours(1));
        testBooking2.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getBookingStats() Tests")
    class GetBookingStatsTests {

        @Test
        @DisplayName("Should return overall booking statistics")
        void shouldReturnOverallBookingStats() {
            // Arrange
            when(bookingRepository.count()).thenReturn(100L);
            when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(20L);
            when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(50L);
            when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(15L);
            when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(15L);
            when(bookingRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testBooking1, testBooking2));

            // Act
            BookingStatsResponse response = statisticsService.getBookingStats();

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getTotalBookings());
            assertEquals(70L, response.getActiveBookings()); // 20 + 50
            assertEquals(20L, response.getPendingBookings());
            assertEquals(50L, response.getConfirmedBookings());
            assertEquals(15L, response.getCancelledBookings());
            assertEquals(15L, response.getCompletedBookings());
            assertTrue(response.getCompletionRate() > 0);
            assertTrue(response.getCancellationRate() > 0);
        }

        @Test
        @DisplayName("Should return zero statistics when no bookings")
        void shouldReturnZeroStatsWhenNoBookings() {
            // Arrange
            when(bookingRepository.count()).thenReturn(0L);
            when(bookingRepository.countByStatus(any(BookingStatus.class))).thenReturn(0L);
            when(bookingRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            BookingStatsResponse response = statisticsService.getBookingStats();

            // Assert
            assertNotNull(response);
            assertEquals(0L, response.getTotalBookings());
            assertEquals(0L, response.getActiveBookings());
            assertEquals(0.0, response.getCompletionRate());
            assertEquals(0.0, response.getCancellationRate());
        }

        @Test
        @DisplayName("Should calculate rates correctly")
        void shouldCalculateRatesCorrectly() {
            // Arrange
            when(bookingRepository.count()).thenReturn(100L);
            when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(10L);
            when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(20L);
            when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(30L);
            when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(40L);
            when(bookingRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            BookingStatsResponse response = statisticsService.getBookingStats();

            // Assert
            assertEquals(40.0, response.getCompletionRate()); // 40/100 * 100
            assertEquals(30.0, response.getCancellationRate()); // 30/100 * 100
            assertEquals(20.0, response.getConfirmationRate()); // 20/100 * 100
        }
    }

    @Nested
    @DisplayName("getMerchantBookingStats() Tests")
    class GetMerchantBookingStatsTests {

        @Test
        @DisplayName("Should return merchant-specific booking statistics")
        void shouldReturnMerchantBookingStats() {
            // Arrange
            when(bookingRepository.findByMerchantId(1L))
                    .thenReturn(Arrays.asList(testBooking1, testBooking2));
            when(bookingRepository.findByMerchantIdAndCreatedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testBooking1));

            // Act
            BookingStatsResponse response = statisticsService.getMerchantBookingStats(1L);

            // Assert
            assertNotNull(response);
            assertEquals(2L, response.getTotalBookings());
            assertEquals(2L, response.getActiveBookings());
        }

        @Test
        @DisplayName("Should return empty stats for merchant with no bookings")
        void shouldReturnEmptyStatsForMerchantWithNoBookings() {
            // Arrange
            when(bookingRepository.findByMerchantId(1L)).thenReturn(Collections.emptyList());
            when(bookingRepository.findByMerchantIdAndCreatedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            BookingStatsResponse response = statisticsService.getMerchantBookingStats(1L);

            // Assert
            assertNotNull(response);
            assertEquals(0L, response.getTotalBookings());
            assertEquals(0.0, response.getCompletionRate());
        }
    }

    @Nested
    @DisplayName("getUserStats() Tests")
    class GetUserStatsTests {

        @Test
        @DisplayName("Should return user statistics")
        void shouldReturnUserStats() {
            // Arrange
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.findByEnabledTrue()).thenReturn(Arrays.asList(testUser));
            when(userRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(5L);
            when(userRepository.countByRole(UserRole.MERCHANT)).thenReturn(15L);
            when(userRepository.countByRole(UserRole.USER)).thenReturn(80L);
            when(bookingRepository.findAll()).thenReturn(Arrays.asList(testBooking1, testBooking2));
            when(serviceItemRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            UserStatsResponse response = statisticsService.getUserStats();

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getTotalUsers());
            assertEquals(1L, response.getEnabledUsers());
            assertEquals(5L, response.getAdminCount());
            assertEquals(15L, response.getMerchantCount());
            assertEquals(80L, response.getUserCount());
        }

        @Test
        @DisplayName("Should calculate active users with bookings")
        void shouldCalculateActiveUsersWithBookings() {
            // Arrange
            User user2 = new User();
            user2.setId(2L);

            when(userRepository.count()).thenReturn(2L);
            when(userRepository.findByEnabledTrue()).thenReturn(Arrays.asList(testUser, user2));
            when(userRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            when(userRepository.countByRole(any(UserRole.class))).thenReturn(0L);
            when(bookingRepository.findAll()).thenReturn(Arrays.asList(testBooking1, testBooking2));
            when(serviceItemRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            UserStatsResponse response = statisticsService.getUserStats();

            // Assert
            assertEquals(1L, response.getActiveUsersWithBookings()); // Only testUser has bookings
        }
    }

    @Nested
    @DisplayName("getSystemStats() Tests")
    class GetSystemStatsTests {

        @Test
        @DisplayName("Should return system statistics")
        void shouldReturnSystemStats() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("100");
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(anyString(), anyLong(), anyLong()))
                    .thenReturn(Arrays.asList("50", "100", "150"));
            when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

            // Act
            SystemStatsResponse response = statisticsService.getSystemStats();

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getTodayApiCalls());
            assertTrue(response.getAverageResponseTimeMs() > 0);
            assertNotNull(response.getHeapUsedMb());
            assertNotNull(response.getUptimeSeconds());
        }
    }

    @Nested
    @DisplayName("Metric Recording Tests")
    class MetricRecordingTests {

        @Test
        @DisplayName("Should record API call")
        void shouldRecordApiCall() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // Act
            statisticsService.recordApiCall();

            // Assert
            verify(valueOperations, times(2)).increment(anyString());
            verify(redisTemplate, times(2)).expire(anyString(), any());
        }

        @Test
        @DisplayName("Should record client error (4xx)")
        void shouldRecordClientError() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // Act
            statisticsService.recordError(404);

            // Assert
            verify(valueOperations, atLeast(2)).increment(anyString());
        }

        @Test
        @DisplayName("Should record server error (5xx)")
        void shouldRecordServerError() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // Act
            statisticsService.recordError(500);

            // Assert
            verify(valueOperations, atLeast(2)).increment(anyString());
        }

        @Test
        @DisplayName("Should record response time")
        void shouldRecordResponseTime() {
            // Arrange
            when(redisTemplate.opsForList()).thenReturn(listOperations);

            // Act
            statisticsService.recordResponseTime(100);

            // Assert
            verify(listOperations).rightPush(anyString(), anyString());
            verify(listOperations).trim(anyString(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("generateDailySummary() Tests")
    class GenerateDailySummaryTests {

        @Test
        @DisplayName("Should generate daily summary")
        void shouldGenerateDailySummary() {
            // Arrange
            LocalDate date = LocalDate.now().minusDays(1);
            when(bookingRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testBooking1, testBooking2));
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);

            // Act
            statisticsService.generateDailySummary(date);

            // Assert
            verify(bookingRepository).findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
            verify(hashOperations).putAll(anyString(), any(Map.class));
            verify(redisTemplate).expire(anyString(), any());
        }
    }
}
