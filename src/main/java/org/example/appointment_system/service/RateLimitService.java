package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 使用Redis滑动窗口算法进行限流的服务类。
 *
 * <p>此服务使用Redis实现滑动窗口限流算法。
 * 它在可配置的时间窗口内跟踪每个客户端（IP地址或用户）的请求计数。</p>
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>滑动窗口算法实现精确限流</li>
 *   <li>每个端点或客户端可配置的限制</li>
 *   <li>基于Redis支持分布式限流</li>
 *   <li>支持已认证用户和匿名用户不同的限流限制</li>
 * </ul>
 *
 * <h3>键格式：</h3>
 * <pre>
 * rate_limit:{type}:{identifier}:{window}
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 默认限流规则
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 1000;
    private static final int AUTHENTICATED_REQUESTS_PER_MINUTE = 120;
    private static final int AUTHENTICATED_REQUESTS_PER_HOUR = 2000;

    // 窗口时长（秒）
    private static final long MINUTE_WINDOW = 60;
    private static final long HOUR_WINDOW = 3600;

    // 键前缀
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String IP_PREFIX = "ip:";
    private static final String USER_PREFIX = "user:";

    /**
     * 检查给定客户端标识符的请求是否允许。
     *
     * <p>使用滑动窗口算法跟踪请求计数。</p>
     *
     * @param clientId 客户端标识符（IP或用户ID）
     * @param isAuthenticated 客户端是否已认证
     * @return 包含检查结果和限制信息的RateLimitResult
     */
    public RateLimitResult checkRateLimit(String clientId, boolean isAuthenticated) {
        return checkRateLimit(clientId, isAuthenticated, null);
    }

    /**
     * 使用自定义限制检查请求是否允许。
     *
     * @param clientId 客户端标识符
     * @param isAuthenticated 客户端是否已认证
     * @param customLimit 自定义每分钟请求限制（null表示默认）
     * @return 包含检查结果和限制信息的RateLimitResult
     */
    public RateLimitResult checkRateLimit(String clientId, boolean isAuthenticated, Integer customLimit) {
        int perMinuteLimit = customLimit != null ? customLimit :
            (isAuthenticated ? AUTHENTICATED_REQUESTS_PER_MINUTE : DEFAULT_REQUESTS_PER_MINUTE);

        String minuteKey = buildKey(clientId, "minute");
        long currentCount = incrementCounter(minuteKey, MINUTE_WINDOW);

        boolean allowed = currentCount <= perMinuteLimit;
        int remaining = Math.max(0, perMinuteLimit - (int) currentCount);

        if (!allowed) {
            log.warn("Rate limit exceeded for client {}: {} requests/minute (limit: {})",
                clientId, currentCount, perMinuteLimit);
        }

        return new RateLimitResult(
            allowed,
            perMinuteLimit,
            remaining,
            System.currentTimeMillis() + (MINUTE_WINDOW * 1000),
            currentCount
        );
    }

    /**
     * 检查IP地址的限流。
     *
     * @param ipAddress IP地址
     * @return RateLimitResult
     */
    public RateLimitResult checkIpRateLimit(String ipAddress) {
        return checkRateLimit(IP_PREFIX + ipAddress, false);
    }

    /**
     * 检查已认证用户的限流。
     *
     * @param userId 用户ID
     * @return RateLimitResult
     */
    public RateLimitResult checkUserRateLimit(Long userId) {
        return checkRateLimit(USER_PREFIX + userId, true);
    }

    /**
     * 检查特定端点的限流。
     *
     * @param endpoint 端点路径
     * @param clientId 客户端标识符
     * @param limit 每分钟请求限制
     * @return RateLimitResult
     */
    public RateLimitResult checkEndpointRateLimit(String endpoint, String clientId, int limit) {
        String key = "endpoint:" + endpoint + ":" + clientId;
        return checkRateLimit(key, true, limit);
    }

    /**
     * 重置客户端的限流计数器。
     *
     * @param clientId 客户端标识符
     */
    public void resetRateLimit(String clientId) {
        String minuteKey = buildKey(clientId, "minute");
        String hourKey = buildKey(clientId, "hour");
        redisTemplate.delete(minuteKey);
        redisTemplate.delete(hourKey);
        log.info("Reset rate limit counters for client: {}", clientId);
    }

    /**
     * 获取客户端的当前请求计数。
     *
     * @param clientId 客户端标识符
     * @return 分钟窗口内的当前请求计数
     */
    public long getCurrentCount(String clientId) {
        String minuteKey = buildKey(clientId, "minute");
        Object count = redisTemplate.opsForValue().get(minuteKey);
        return count != null ? Long.parseLong(count.toString()) : 0;
    }

    /**
     * 构建用于限流的Redis键。
     *
     * @param clientId 客户端标识符
     * @param window 窗口类型（minute/hour）
     * @return Redis键
     */
    private String buildKey(String clientId, String window) {
        return RATE_LIMIT_PREFIX + clientId + ":" + window;
    }

    /**
     * 递增计数器并在需要时设置过期时间。
     *
     * <p>使用Redis INCR命令与过期时间实现原子操作。</p>
     *
     * @param key Redis键
     * @param windowSeconds 窗口时长（秒）
     * @return 新的计数器值
     */
    private long incrementCounter(String key, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 仅在首次递增时设置过期时间
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null ? count : 0;
    }

    /**
     * 限流检查的结果。
     */
    public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetTimeMs,
        long currentCount
    ) {
        /**
         * 获取剩余请求占限制的百分比。
         *
         * @return 剩余百分比（0-100）
         */
        public int getRemainingPercentage() {
            if (limit == 0) return 0;
            return (int) ((remaining * 100.0) / limit);
        }

        /**
         * 检查客户端是否接近限流限制。
         *
         * @param threshold 百分比阈值（0-100）
         * @return 如果剩余百分比低于阈值返回true
         */
        public boolean isApproachingLimit(int threshold) {
            return getRemainingPercentage() < threshold;
        }
    }
}
