package org.example.appointment_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    @DisplayName("checkRateLimit - should allow request under limit")
    void checkRateLimit_shouldAllowUnderLimit() {
        // Given
        String clientId = "user:1";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(clientId, true);

        // Then
        assertTrue(result.allowed());
        assertEquals(120, result.limit());
        assertEquals(119, result.remaining());
        assertEquals(1, result.currentCount());
        verify(valueOperations).increment(anyString());
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("checkRateLimit - should deny request over limit")
    void checkRateLimit_shouldDenyOverLimit() {
        // Given
        String clientId = "ip:127.0.0.1";
        when(valueOperations.increment(anyString())).thenReturn(61L);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(clientId, false);

        // Then
        assertFalse(result.allowed());
        assertEquals(60, result.limit());
        assertEquals(0, result.remaining());
        assertEquals(61, result.currentCount());
    }

    @Test
    @DisplayName("checkRateLimit - should use custom limit")
    void checkRateLimit_shouldUseCustomLimit() {
        // Given
        String clientId = "user:1";
        int customLimit = 10;
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(clientId, true, customLimit);

        // Then
        assertTrue(result.allowed());
        assertEquals(10, result.limit());
        assertEquals(5, result.remaining());
    }

    @Test
    @DisplayName("checkIpRateLimit - should check IP-based rate limit")
    void checkIpRateLimit_shouldCheckIpRateLimit() {
        // Given
        String ipAddress = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkIpRateLimit(ipAddress);

        // Then
        assertTrue(result.allowed());
        assertEquals(60, result.limit()); // Default for anonymous
    }

    @Test
    @DisplayName("checkUserRateLimit - should check user-based rate limit")
    void checkUserRateLimit_shouldCheckUserRateLimit() {
        // Given
        Long userId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkUserRateLimit(userId);

        // Then
        assertTrue(result.allowed());
        assertEquals(120, result.limit()); // Higher for authenticated
    }

    @Test
    @DisplayName("checkEndpointRateLimit - should check endpoint-specific rate limit")
    void checkEndpointRateLimit_shouldCheckEndpointRateLimit() {
        // Given
        String endpoint = "/api/auth/login";
        String clientId = "ip:127.0.0.1";
        int limit = 5;
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkEndpointRateLimit(endpoint, clientId, limit);

        // Then
        assertTrue(result.allowed());
        assertEquals(5, result.limit());
        assertEquals(2, result.remaining());
    }

    @Test
    @DisplayName("resetRateLimit - should delete rate limit counters")
    void resetRateLimit_shouldDeleteCounters() {
        // Given
        String clientId = "user:1";

        // When
        rateLimitService.resetRateLimit(clientId);

        // Then
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("getCurrentCount - should return current count")
    void getCurrentCount_shouldReturnCurrentCount() {
        // Given
        String clientId = "user:1";
        when(valueOperations.get(anyString())).thenReturn("5");

        // When
        long count = rateLimitService.getCurrentCount(clientId);

        // Then
        assertEquals(5, count);
    }

    @Test
    @DisplayName("getCurrentCount - should return 0 when no count exists")
    void getCurrentCount_shouldReturnZeroWhenNoCount() {
        // Given
        String clientId = "user:1";
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        long count = rateLimitService.getCurrentCount(clientId);

        // Then
        assertEquals(0, count);
    }

    @Test
    @DisplayName("RateLimitResult - should calculate remaining percentage correctly")
    void rateLimitResult_shouldCalculateRemainingPercentage() {
        // Given
        RateLimitService.RateLimitResult result = new RateLimitService.RateLimitResult(
            true, 100, 50, System.currentTimeMillis() + 60000, 50
        );

        // When
        int percentage = result.getRemainingPercentage();

        // Then
        assertEquals(50, percentage);
    }

    @Test
    @DisplayName("RateLimitResult - should detect approaching limit")
    void rateLimitResult_shouldDetectApproachingLimit() {
        // Given - 10% remaining
        RateLimitService.RateLimitResult result = new RateLimitService.RateLimitResult(
            true, 100, 10, System.currentTimeMillis() + 60000, 90
        );

        // When & Then
        assertTrue(result.isApproachingLimit(20)); // Below 20% threshold
        assertFalse(result.isApproachingLimit(5)); // Above 5% threshold
    }

    @Test
    @DisplayName("RateLimitResult - should handle zero limit")
    void rateLimitResult_shouldHandleZeroLimit() {
        // Given
        RateLimitService.RateLimitResult result = new RateLimitService.RateLimitResult(
            false, 0, 0, System.currentTimeMillis() + 60000, 1
        );

        // When
        int percentage = result.getRemainingPercentage();

        // Then
        assertEquals(0, percentage);
    }

    @Test
    @DisplayName("checkRateLimit - should not set expiry on subsequent increments")
    void checkRateLimit_shouldNotSetExpiryOnSubsequentIncrements() {
        // Given
        String clientId = "user:1";
        when(valueOperations.increment(anyString())).thenReturn(2L); // Second request

        // When
        rateLimitService.checkRateLimit(clientId, true);

        // Then
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("checkRateLimit - should handle null increment result")
    void checkRateLimit_shouldHandleNullIncrementResult() {
        // Given
        String clientId = "user:1";
        when(valueOperations.increment(anyString())).thenReturn(null);

        // When
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(clientId, true);

        // Then
        assertTrue(result.allowed());
        assertEquals(0, result.currentCount());
    }

    @Test
    @DisplayName("checkRateLimit - authenticated user should have higher limit")
    void checkRateLimit_authenticatedUserHigherLimit() {
        // Given
        String clientId = "user:1";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When - authenticated
        RateLimitService.RateLimitResult authResult = rateLimitService.checkRateLimit(clientId, true);

        // Then
        assertEquals(120, authResult.limit());
    }

    @Test
    @DisplayName("checkRateLimit - anonymous user should have lower limit")
    void checkRateLimit_anonymousUserLowerLimit() {
        // Given
        String clientId = "ip:127.0.0.1";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When - anonymous
        RateLimitService.RateLimitResult anonResult = rateLimitService.checkRateLimit(clientId, false);

        // Then
        assertEquals(60, anonResult.limit());
    }
}
