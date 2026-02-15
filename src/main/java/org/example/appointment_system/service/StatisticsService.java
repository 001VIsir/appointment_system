package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.response.BookingStatsResponse;
import org.example.appointment_system.dto.response.SystemStatsResponse;
import org.example.appointment_system.dto.response.UserStatsResponse;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for statistics and metrics operations.
 *
 * <p>Provides various statistics including:</p>
 * <ul>
 *   <li>Booking statistics (by date, merchant, status)</li>
 *   <li>User statistics (registrations, activity)</li>
 *   <li>System statistics (API calls, error rates, response times)</li>
 * </ul>
 *
 * <h3>Redis-based Metrics:</h3>
 * <p>This service uses Redis to track API calls, errors, and response times
 * with time-based keys for real-time statistics.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final StringRedisTemplate redisTemplate;

    // Redis key prefixes for metrics
    private static final String API_CALLS_KEY = "stats:api:calls:";
    private static final String ERRORS_KEY = "stats:api:errors:";
    private static final String RESPONSE_TIME_KEY = "stats:api:response:";
    private static final String CLIENT_ERRORS_KEY = "stats:api:client_errors:";
    private static final String SERVER_ERRORS_KEY = "stats:api:server_errors:";

    // ============================================
    // Booking Statistics
    // ============================================

    /**
     * Get overall booking statistics.
     *
     * @return BookingStatsResponse with all booking metrics
     */
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats() {
        return getBookingStats(null, null);
    }

    /**
     * Get booking statistics for a date range.
     *
     * @param startDate start date (inclusive), null for all time
     * @param endDate   end date (inclusive), null for all time
     * @return BookingStatsResponse with booking metrics
     */
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats(@Nullable LocalDate startDate, @Nullable LocalDate endDte) {
        log.debug("Getting booking stats from {} to {}", startDate, endDte);

        // Overall counts
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long activeBookings = pendingBookings + confirmedBookings;

        // Today's statistics
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        List<Booking> todayBookingsList = bookingRepository.findByCreatedAtBetween(todayStart, todayEnd);
        long todayBookings = todayBookingsList.size();
        long todayActive = todayBookingsList.stream().filter(Booking::isActive).count();
        long todayCompleted = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        // Calculate rates
        double completionRate = totalBookings > 0 ? (double) completedBookings / totalBookings * 100 : 0;
        double cancellationRate = totalBookings > 0 ? (double) cancelledBookings / totalBookings * 100 : 0;
        double confirmationRate = totalBookings > 0
                ? (double) confirmedBookings / totalBookings * 100 : 0;

        // Date range statistics
        long periodBookings = 0;
        Map<LocalDate, Long> dailyBookings = new HashMap<>();

        if (startDate != null && endDte != null) {
            LocalDateTime periodStart = startDate.atStartOfDay();
            LocalDateTime periodEnd = endDte.atTime(LocalTime.MAX);
            List<Booking> periodBookingsList = bookingRepository.findByCreatedAtBetween(periodStart, periodEnd);
            periodBookings = periodBookingsList.size();

            // Group by date
            periodBookingsList.forEach(booking -> {
                LocalDate date = booking.getCreatedAt().toLocalDate();
                dailyBookings.merge(date, 1L, Long::sum);
            });
        }

        return BookingStatsResponse.builder()
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .pendingBookings(pendingBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .completedBookings(completedBookings)
                .todayBookings(todayBookings)
                .todayActiveBookings(todayActive)
                .todayCompletedBookings(todayCompleted)
                .startDate(startDate)
                .endDate(endDte)
                .periodBookings(periodBookings)
                .dailyBookings(dailyBookings)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .cancellationRate(Math.round(cancellationRate * 100.0) / 100.0)
                .confirmationRate(Math.round(confirmationRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Get booking statistics for a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return BookingStatsResponse with merchant-specific metrics
     */
    @Transactional(readOnly = true)
    public BookingStatsResponse getMerchantBookingStats(Long merchantId) {
        log.debug("Getting booking stats for merchant {}", merchantId);

        List<Booking> merchantBookings = bookingRepository.findByMerchantId(merchantId);
        long totalBookings = merchantBookings.size();

        long pendingBookings = merchantBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long confirmedBookings = merchantBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelledBookings = merchantBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long completedBookings = merchantBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long activeBookings = pendingBookings + confirmedBookings;

        // Today's statistics for merchant
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        List<Booking> todayBookingsList = bookingRepository.findByMerchantIdAndCreatedAtBetween(
                merchantId, todayStart, todayEnd);
        long todayBookings = todayBookingsList.size();
        long todayActive = todayBookingsList.stream().filter(Booking::isActive).count();
        long todayCompleted = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        // Calculate rates
        double completionRate = totalBookings > 0 ? (double) completedBookings / totalBookings * 100 : 0;
        double cancellationRate = totalBookings > 0 ? (double) cancelledBookings / totalBookings * 100 : 0;
        double confirmationRate = totalBookings > 0
                ? (double) confirmedBookings / totalBookings * 100 : 0;

        return BookingStatsResponse.builder()
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .pendingBookings(pendingBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .completedBookings(completedBookings)
                .todayBookings(todayBookings)
                .todayActiveBookings(todayActive)
                .todayCompletedBookings(todayCompleted)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .cancellationRate(Math.round(cancellationRate * 100.0) / 100.0)
                .confirmationRate(Math.round(confirmationRate * 100.0) / 100.0)
                .build();
    }

    // ============================================
    // User Statistics
    // ============================================

    /**
     * Get user statistics.
     *
     * @return UserStatsResponse with user metrics
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        log.debug("Getting user stats");

        // Overall user counts
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.findByEnabledTrue().size();
        long disabledUsers = userRepository.findByEnabledFalse().size();

        // Role-based counts
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        long merchantCount = userRepository.countByRole(UserRole.MERCHANT);
        long userCount = userRepository.countByRole(UserRole.USER);

        // Registration statistics (approximation based on creation time)
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);

        // For new user counts, we'll use a simple approximation
        // In a real system, you'd have a createdAt field on User and query it
        long todayNewUsers = 0; // Would need createdAt on User entity
        long weekNewUsers = 0;
        long monthNewUsers = 0;

        // Activity statistics
        long activeUsersWithBookings = bookingRepository.findAll().stream()
                .map(b -> b.getUser().getId())
                .distinct()
                .count();

        long activeMerchantsWithServices = serviceItemRepository.findAll().stream()
                .map(s -> s.getMerchant().getId())
                .distinct()
                .count();

        // Average bookings per user
        double averageBookingsPerUser = totalUsers > 0
                ? (double) bookingRepository.count() / totalUsers : 0;

        return UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .enabledUsers(enabledUsers)
                .disabledUsers(disabledUsers)
                .adminCount(adminCount)
                .merchantCount(merchantCount)
                .userCount(userCount)
                .todayNewUsers(todayNewUsers)
                .weekNewUsers(weekNewUsers)
                .monthNewUsers(monthNewUsers)
                .activeUsersWithBookings(activeUsersWithBookings)
                .activeMerchantsWithServices(activeMerchantsWithServices)
                .averageBookingsPerUser(Math.round(averageBookingsPerUser * 100.0) / 100.0)
                .build();
    }

    // ============================================
    // System Statistics
    // ============================================

    /**
     * Get system statistics.
     *
     * @return SystemStatsResponse with system metrics
     */
    public SystemStatsResponse getSystemStats() {
        log.debug("Getting system stats");

        // API call statistics from Redis
        long todayApiCalls = getTodayMetric(API_CALLS_KEY);
        long lastHourApiCalls = getLastHourMetric(API_CALLS_KEY);
        double apiCallsPerMinute = lastHourApiCalls / 60.0;

        // Error statistics from Redis
        long todayErrors = getTodayMetric(ERRORS_KEY);
        long lastHourErrors = getLastHourMetric(ERRORS_KEY);
        long clientErrors = getTodayMetric(CLIENT_ERRORS_KEY);
        long serverErrors = getTodayMetric(SERVER_ERRORS_KEY);

        double errorRate = todayApiCalls > 0
                ? (double) todayErrors / todayApiCalls * 100 : 0;

        // Response time statistics from Redis
        double averageResponseTime = getAverageResponseTime();
        long maxResponseTime = getMaxResponseTime();
        long minResponseTime = getMinResponseTime();
        long p95ResponseTime = getP95ResponseTime();

        // Resource statistics
        long activeSessions = getActiveSessionCount();

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = runtimeBean.getUptime() / 1000;

        return SystemStatsResponse.builder()
                .todayApiCalls(todayApiCalls)
                .lastHourApiCalls(lastHourApiCalls)
                .apiCallsPerMinute(Math.round(apiCallsPerMinute * 100.0) / 100.0)
                .todayErrors(todayErrors)
                .lastHourErrors(lastHourErrors)
                .errorRate(Math.round(errorRate * 100.0) / 100.0)
                .clientErrors(clientErrors)
                .serverErrors(serverErrors)
                .averageResponseTimeMs(Math.round(averageResponseTime * 100.0) / 100.0)
                .maxResponseTimeMs(maxResponseTime)
                .minResponseTimeMs(minResponseTime)
                .p95ResponseTimeMs(p95ResponseTime)
                .activeSessions(activeSessions)
                .heapUsedMb(heapUsed)
                .heapMaxMb(heapMax)
                .uptimeSeconds(uptimeSeconds)
                .build();
    }

    // ============================================
    // Metric Recording Methods
    // ============================================

    /**
     * Record an API call.
     */
    public void recordApiCall() {
        String todayKey = API_CALLS_KEY + LocalDate.now();
        String hourKey = API_CALLS_KEY + "hour:" + LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(2));

        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, Duration.ofDays(1));
    }

    /**
     * Record an error.
     *
     * @param statusCode the HTTP status code
     */
    public void recordError(int statusCode) {
        String todayKey = ERRORS_KEY + LocalDate.now();
        String hourKey = ERRORS_KEY + "hour:" + LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(2));

        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, Duration.ofDays(1));

        // Track client vs server errors
        if (statusCode >= 400 && statusCode < 500) {
            String clientKey = CLIENT_ERRORS_KEY + LocalDate.now();
            redisTemplate.opsForValue().increment(clientKey);
            redisTemplate.expire(clientKey, Duration.ofDays(2));
        } else if (statusCode >= 500) {
            String serverKey = SERVER_ERRORS_KEY + LocalDate.now();
            redisTemplate.opsForValue().increment(serverKey);
            redisTemplate.expire(serverKey, Duration.ofDays(2));
        }
    }

    /**
     * Record response time.
     *
     * @param responseTimeMs the response time in milliseconds
     */
    public void recordResponseTime(long responseTimeMs) {
        String todayKey = RESPONSE_TIME_KEY + LocalDate.now();

        // Store in a Redis list for calculating statistics
        redisTemplate.opsForList().rightPush(todayKey, String.valueOf(responseTimeMs));

        // Trim the list to keep only last 10000 entries
        redisTemplate.opsForList().trim(todayKey, -10000, -1);

        // Set expiry
        redisTemplate.expire(todayKey, Duration.ofDays(2));
    }

    // ============================================
    // Helper Methods for Redis Metrics
    // ============================================

    private long getTodayMetric(String keyPrefix) {
        String key = keyPrefix + LocalDate.now();
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    private long getLastHourMetric(String keyPrefix) {
        String hourKey = keyPrefix + "hour:" + LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        String value = redisTemplate.opsForValue().get(hourKey);
        return value != null ? Long.parseLong(value) : 0;
    }

    private double getAverageResponseTime() {
        String key = RESPONSE_TIME_KEY + LocalDate.now();
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return values.stream()
                .mapToLong(Long::parseLong)
                .average()
                .orElse(0);
    }

    private long getMaxResponseTime() {
        String key = RESPONSE_TIME_KEY + LocalDate.now();
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return values.stream()
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0);
    }

    private long getMinResponseTime() {
        String key = RESPONSE_TIME_KEY + LocalDate.now();
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return values.stream()
                .mapToLong(Long::parseLong)
                .min()
                .orElse(0);
    }

    private long getP95ResponseTime() {
        String key = RESPONSE_TIME_KEY + LocalDate.now();
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        if (values == null || values.isEmpty()) {
            return 0;
        }

        List<Long> sortedValues = values.stream()
                .mapToLong(Long::parseLong)
                .sorted()
                .boxed()
                .toList();

        int p95Index = (int) Math.ceil(sortedValues.size() * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, sortedValues.size() - 1));

        return sortedValues.get(p95Index);
    }

    private long getActiveSessionCount() {
        // Count sessions in Redis with the session namespace
        // This is an approximation - actual implementation depends on Spring Session configuration
        try {
            var keys = redisTemplate.keys("appointment:session:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("Failed to count active sessions", e);
            return 0;
        }
    }

    // ============================================
    // Daily Summary Generation
    // ============================================

    /**
     * Generate daily statistics summary.
     * Called by the scheduled task.
     *
     * @param date the date to generate summary for
     */
    public void generateDailySummary(LocalDate date) {
        log.info("Generating daily summary for {}", date);

        // Generate booking stats for the date
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        List<Booking> dayBookings = bookingRepository.findByCreatedAtBetween(dayStart, dayEnd);

        long totalBookings = dayBookings.size();
        long pending = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long confirmed = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelled = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long completed = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        log.info("Daily summary for {}: total={}, pending={}, confirmed={}, cancelled={}, completed={}",
                date, totalBookings, pending, confirmed, cancelled, completed);

        // Store summary in Redis for quick access
        String summaryKey = "stats:daily:" + date;
        Map<String, String> summary = Map.of(
                "total", String.valueOf(totalBookings),
                "pending", String.valueOf(pending),
                "confirmed", String.valueOf(confirmed),
                "cancelled", String.valueOf(cancelled),
                "completed", String.valueOf(completed)
        );

        redisTemplate.opsForHash().putAll(summaryKey, summary);
        redisTemplate.expire(summaryKey, Duration.ofDays(90)); // Keep for 90 days
    }
}
