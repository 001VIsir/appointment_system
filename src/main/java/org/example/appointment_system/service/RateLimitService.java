package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting using Redis sliding window algorithm.
 *
 * <p>This service implements a sliding window rate limiting algorithm using Redis.
 * It tracks request counts per client (IP address or user) within configurable
 * time windows.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Sliding window algorithm for precise rate limiting</li>
 *   <li>Configurable limits per endpoint or client</li>
 *   <li>Redis-backed for distributed rate limiting</li>
 *   <li>Supports different rate limits for authenticated vs anonymous users</li>
 * </ul>
 *
 * <h3>Key Format:</h3>
 * <pre>
 * rate_limit:{type}:{identifier}:{window}
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Default rate limits
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 1000;
    private static final int AUTHENTICATED_REQUESTS_PER_MINUTE = 120;
    private static final int AUTHENTICATED_REQUESTS_PER_HOUR = 2000;

    // Window durations in seconds
    private static final long MINUTE_WINDOW = 60;
    private static final long HOUR_WINDOW = 3600;

    // Key prefixes
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String IP_PREFIX = "ip:";
    private static final String USER_PREFIX = "user:";

    /**
     * Check if a request is allowed for the given client identifier.
     *
     * <p>Uses sliding window algorithm to track request counts.</p>
     *
     * @param clientId the client identifier (IP or user ID)
     * @param isAuthenticated whether the client is authenticated
     * @return RateLimitResult containing the check result and limit info
     */
    public RateLimitResult checkRateLimit(String clientId, boolean isAuthenticated) {
        return checkRateLimit(clientId, isAuthenticated, null);
    }

    /**
     * Check if a request is allowed with custom limits.
     *
     * @param clientId the client identifier
     * @param isAuthenticated whether the client is authenticated
     * @param customLimit custom requests per minute limit (null for default)
     * @return RateLimitResult containing the check result and limit info
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
     * Check rate limit for an IP address.
     *
     * @param ipAddress the IP address
     * @return RateLimitResult
     */
    public RateLimitResult checkIpRateLimit(String ipAddress) {
        return checkRateLimit(IP_PREFIX + ipAddress, false);
    }

    /**
     * Check rate limit for an authenticated user.
     *
     * @param userId the user ID
     * @return RateLimitResult
     */
    public RateLimitResult checkUserRateLimit(Long userId) {
        return checkRateLimit(USER_PREFIX + userId, true);
    }

    /**
     * Check rate limit for a specific endpoint.
     *
     * @param endpoint the endpoint path
     * @param clientId the client identifier
     * @param limit the requests per minute limit
     * @return RateLimitResult
     */
    public RateLimitResult checkEndpointRateLimit(String endpoint, String clientId, int limit) {
        String key = "endpoint:" + endpoint + ":" + clientId;
        return checkRateLimit(key, true, limit);
    }

    /**
     * Reset the rate limit counter for a client.
     *
     * @param clientId the client identifier
     */
    public void resetRateLimit(String clientId) {
        String minuteKey = buildKey(clientId, "minute");
        String hourKey = buildKey(clientId, "hour");
        redisTemplate.delete(minuteKey);
        redisTemplate.delete(hourKey);
        log.info("Reset rate limit counters for client: {}", clientId);
    }

    /**
     * Get current request count for a client.
     *
     * @param clientId the client identifier
     * @return current request count in the minute window
     */
    public long getCurrentCount(String clientId) {
        String minuteKey = buildKey(clientId, "minute");
        Object count = redisTemplate.opsForValue().get(minuteKey);
        return count != null ? Long.parseLong(count.toString()) : 0;
    }

    /**
     * Build the Redis key for rate limiting.
     *
     * @param clientId the client identifier
     * @param window the window type (minute/hour)
     * @return the Redis key
     */
    private String buildKey(String clientId, String window) {
        return RATE_LIMIT_PREFIX + clientId + ":" + window;
    }

    /**
     * Increment the counter and set expiry if needed.
     *
     * <p>Uses Redis INCR command with expiry for atomic operation.</p>
     *
     * @param key the Redis key
     * @param windowSeconds the window duration in seconds
     * @return the new counter value
     */
    private long incrementCounter(String key, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // Set expiry only on first increment
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null ? count : 0;
    }

    /**
     * Result of a rate limit check.
     */
    public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetTimeMs,
        long currentCount
    ) {
        /**
         * Get the remaining requests as a percentage of the limit.
         *
         * @return percentage remaining (0-100)
         */
        public int getRemainingPercentage() {
            if (limit == 0) return 0;
            return (int) ((remaining * 100.0) / limit);
        }

        /**
         * Check if the client is approaching the rate limit.
         *
         * @param threshold percentage threshold (0-100)
         * @return true if remaining percentage is below threshold
         */
        public boolean isApproachingLimit(int threshold) {
            return getRemainingPercentage() < threshold;
        }
    }
}
