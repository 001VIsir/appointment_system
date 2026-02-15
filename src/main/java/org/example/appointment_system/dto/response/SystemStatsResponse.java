package org.example.appointment_system.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for system-wide statistics.
 *
 * <p>Contains system metrics including:</p>
 * <ul>
 *   <li>API call statistics</li>
 *   <li>Error rate statistics</li>
 *   <li>Response time statistics</li>
 * </ul>
 */
@Data
@Builder
public class SystemStatsResponse {

    // ============================================
    // API Call Statistics
    // ============================================

    /**
     * Total API calls today.
     */
    private long todayApiCalls;

    /**
     * API calls in the last hour.
     */
    private long lastHourApiCalls;

    /**
     * API calls per minute average.
     */
    private double apiCallsPerMinute;

    // ============================================
    // Error Rate Statistics
    // ============================================

    /**
     * Total errors today.
     */
    private long todayErrors;

    /**
     * Errors in the last hour.
     */
    private long lastHourErrors;

    /**
     * Error rate (percentage).
     */
    private double errorRate;

    /**
     * 4xx errors (client errors).
     */
    private long clientErrors;

    /**
     * 5xx errors (server errors).
     */
    private long serverErrors;

    // ============================================
    // Response Time Statistics
    // ============================================

    /**
     * Average response time in milliseconds.
     */
    private double averageResponseTimeMs;

    /**
     * Maximum response time in milliseconds.
     */
    private long maxResponseTimeMs;

    /**
     * Minimum response time in milliseconds.
     */
    private long minResponseTimeMs;

    /**
     * P95 response time in milliseconds.
     */
    private long p95ResponseTimeMs;

    // ============================================
    // Resource Statistics
    // ============================================

    /**
     * Number of active sessions.
     */
    private long activeSessions;

    /**
     * JVM heap used (MB).
     */
    private long heapUsedMb;

    /**
     * JVM heap max (MB).
     */
    private long heapMaxMb;

    /**
     * System uptime in seconds.
     */
    private long uptimeSeconds;
}
