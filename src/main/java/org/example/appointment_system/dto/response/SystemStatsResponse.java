package org.example.appointment_system.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 系统级统计响应数据传输对象。
 *
 * <p>包含系统指标，包括：</p>
 * <ul>
 *   <li>API调用统计</li>
 *   <li>错误率统计</li>
 *   <li>响应时间统计</li>
 * </ul>
 */
@Data
@Builder
public class SystemStatsResponse {

    // ============================================
    // API调用统计
    // ============================================

    /**
     * 今日API调用总数。
     */
    private long todayApiCalls;

    /**
     * 上小时的API调用数。
     */
    private long lastHourApiCalls;

    /**
     * 每分钟平均API调用数。
     */
    private double apiCallsPerMinute;

    // ============================================
    // 错误率统计
    // ============================================

    /**
     * 今日错误总数。
     */
    private long todayErrors;

    /**
     * 上小时的错误数。
     */
    private long lastHourErrors;

    /**
     * 错误率（百分比）。
     */
    private double errorRate;

    /**
     * 4xx错误（客户端错误）。
     */
    private long clientErrors;

    /**
     * 5xx错误（服务端错误）。
     */
    private long serverErrors;

    // ============================================
    // 响应时间统计
    // ============================================

    /**
     * 平均响应时间（毫秒）。
     */
    private double averageResponseTimeMs;

    /**
     * 最大响应时间（毫秒）。
     */
    private long maxResponseTimeMs;

    /**
     * 最小响应时间（毫秒）。
     */
    private long minResponseTimeMs;

    /**
     * P95响应时间（毫秒）。
     */
    private long p95ResponseTimeMs;

    // ============================================
    // 资源统计
    // ============================================

    /**
     * 活跃会话数。
     */
    private long activeSessions;

    /**
     * JVM堆内存使用量（MB）。
     */
    private long heapUsedMb;

    /**
     * JVM堆内存最大值（MB）。
     */
    private long heapMaxMb;

    /**
     * 系统运行时间（秒）。
     */
    private long uptimeSeconds;
}
