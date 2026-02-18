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
 * 统计和指标操作服务类。
 *
 * <p>提供各种统计数据，包括：</p>
 * <ul>
 *   <li>预约统计（按日期、商家、状态）</li>
 *   <li>用户统计（注册、活动）</li>
 *   <li>系统统计（API调用、错误率、响应时间）</li>
 * </ul>
 *
 * <h3>基于Redis的指标：</h3>
 * <p>此服务使用Redis跟踪API调用、错误和响应时间，
 * 使用时间键进行实时统计。</p>
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

    // 指标Redis键前缀
    private static final String API_CALLS_KEY = "stats:api:calls:";
    private static final String ERRORS_KEY = "stats:api:errors:";
    private static final String RESPONSE_TIME_KEY = "stats:api:response:";
    private static final String CLIENT_ERRORS_KEY = "stats:api:client_errors:";
    private static final String SERVER_ERRORS_KEY = "stats:api:server_errors:";

    // ============================================
    // 预约统计
    // ============================================

    /**
     * 获取总体预约统计。
     *
     * @return 包含所有预约指标的BookingStatsResponse
     */
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats() {
        return getBookingStats(null, null);
    }

    /**
     * 获取日期范围的预约统计。
     *
     * @param startDate 开始日期（包含），null表示所有时间
     * @param endDate   结束日期（包含），null表示所有时间
     * @return 包含预约指标的BookingStatsResponse
     */
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats(@Nullable LocalDate startDate, @Nullable LocalDate endDte) {
        log.debug("Getting booking stats from {} to {}", startDate, endDte);

        // 总计数
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long activeBookings = pendingBookings + confirmedBookings;

        // 今日统计
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        List<Booking> todayBookingsList = bookingRepository.findByCreatedAtBetween(todayStart, todayEnd);
        long todayBookings = todayBookingsList.size();
        long todayActive = todayBookingsList.stream().filter(Booking::isActive).count();
        long todayCompleted = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        // 计算比率
        double completionRate = totalBookings > 0 ? (double) completedBookings / totalBookings * 100 : 0;
        double cancellationRate = totalBookings > 0 ? (double) cancelledBookings / totalBookings * 100 : 0;
        double confirmationRate = totalBookings > 0
                ? (double) confirmedBookings / totalBookings * 100 : 0;

        // 日期范围统计
        long periodBookings = 0;
        Map<LocalDate, Long> dailyBookings = new HashMap<>();

        if (startDate != null && endDte != null) {
            LocalDateTime periodStart = startDate.atStartOfDay();
            LocalDateTime periodEnd = endDte.atTime(LocalTime.MAX);
            List<Booking> periodBookingsList = bookingRepository.findByCreatedAtBetween(periodStart, periodEnd);
            periodBookings = periodBookingsList.size();

            // 按日期分组
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
     * 获取特定商家的预约统计。
     *
     * @param merchantId 商家ID
     * @return 包含商家特定指标的BookingStatsResponse
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

        // 今日统计 for merchant
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        List<Booking> todayBookingsList = bookingRepository.findByMerchantIdAndCreatedAtBetween(
                merchantId, todayStart, todayEnd);
        long todayBookings = todayBookingsList.size();
        long todayActive = todayBookingsList.stream().filter(Booking::isActive).count();
        long todayCompleted = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        // 计算比率
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
    // 用户统计
    // ============================================

    /**
     * 获取用户统计。
     *
     * @return 包含用户指标的UserStatsResponse
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        log.debug("Getting user stats");

        // 总用户数
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.findByEnabledTrue().size();
        long disabledUsers = userRepository.findByEnabledFalse().size();

        // 基于角色的计数
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        long merchantCount = userRepository.countByRole(UserRole.MERCHANT);
        long userCount = userRepository.countByRole(UserRole.USER);

        // 注册统计（基于创建时间的近似值）
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);

        // 对于新用户计数，我们将使用一个简单的近似值
        // 在实际系统中，你会在User实体上有createdAt字段并查询它
        long todayNewUsers = 0; // 需要User实体上的createdAt
        long weekNewUsers = 0;
        long monthNewUsers = 0;

        // 活动统计
        long activeUsersWithBookings = bookingRepository.findAll().stream()
                .map(b -> b.getUser().getId())
                .distinct()
                .count();

        long activeMerchantsWithServices = serviceItemRepository.findAll().stream()
                .map(s -> s.getMerchant().getId())
                .distinct()
                .count();

        // 每用户平均预约数
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
    // 系统统计
    // ============================================

    /**
     * 获取系统统计。
     *
     * @return 包含系统指标的SystemStatsResponse
     */
    public SystemStatsResponse getSystemStats() {
        log.debug("Getting system stats");

        // API call statistics from Redis
        long todayApiCalls = getTodayMetric(API_CALLS_KEY);
        long lastHourApiCalls = getLastHourMetric(API_CALLS_KEY);
        double apiCallsPerMinute = lastHourApiCalls / 60.0;

        // 来自Redis的错误统计
        long todayErrors = getTodayMetric(ERRORS_KEY);
        long lastHourErrors = getLastHourMetric(ERRORS_KEY);
        long clientErrors = getTodayMetric(CLIENT_ERRORS_KEY);
        long serverErrors = getTodayMetric(SERVER_ERRORS_KEY);

        double errorRate = todayApiCalls > 0
                ? (double) todayErrors / todayApiCalls * 100 : 0;

        // 来自Redis的响应时间统计
        double averageResponseTime = getAverageResponseTime();
        long maxResponseTime = getMaxResponseTime();
        long minResponseTime = getMinResponseTime();
        long p95ResponseTime = getP95ResponseTime();

        // 资源统计
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
    // 指标记录方法
    // ============================================

    /**
     * 记录API调用。
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
     * 记录错误。
     *
     * @param statusCode HTTP状态码
     */
    public void recordError(int statusCode) {
        String todayKey = ERRORS_KEY + LocalDate.now();
        String hourKey = ERRORS_KEY + "hour:" + LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(2));

        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, Duration.ofDays(1));

        // 跟踪客户端与服务器错误
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
     * 记录响应时间。
     *
     * @param responseTimeMs 响应时间（毫秒）
     */
    public void recordResponseTime(long responseTimeMs) {
        String todayKey = RESPONSE_TIME_KEY + LocalDate.now();

        // 存储在Redis列表中用于计算统计
        redisTemplate.opsForList().rightPush(todayKey, String.valueOf(responseTimeMs));

        // 修剪列表只保留最后10000条记录
        redisTemplate.opsForList().trim(todayKey, -10000, -1);

        // 设置过期时间
        redisTemplate.expire(todayKey, Duration.ofDays(2));
    }

    // ============================================
    // Redis指标的辅助方法 =====================================
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
        // 统计Redis中会话命名空间下的会话数
        // 这是一个近似值 - 实际实现取决于Spring Session配置
        try {
            var keys = redisTemplate.keys("appointment:session:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("Failed to count active sessions", e);
            return 0;
        }
    }

    // ============================================
    // 每日汇总生成 =====================================
    // ============================================

    /**
     * 生成每日统计摘要。
     * 由定时任务调用。
     *
     * @param date 生成摘要的日期
     */
    public void generateDailySummary(LocalDate date) {
        log.info("Generating daily summary for {}", date);

        // 生成当天的预约统计
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

        // 将汇总存储到Redis以便快速访问
        String summaryKey = "stats:daily:" + date;
        Map<String, String> summary = Map.of(
                "total", String.valueOf(totalBookings),
                "pending", String.valueOf(pending),
                "confirmed", String.valueOf(confirmed),
                "cancelled", String.valueOf(cancelled),
                "completed", String.valueOf(completed)
        );

        redisTemplate.opsForHash().putAll(summaryKey, summary);
        redisTemplate.expire(summaryKey, Duration.ofDays(90)); // 保留90天
    }
}
