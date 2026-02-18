package org.example.appointment_system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 预约统计响应数据传输对象。
 *
 * <p>包含各种预约指标和统计数据：</p>
 * <ul>
 *   <li>按状态统计的预约总数</li>
 *   <li>今日预约统计</li>
 *   <li>日期范围统计</li>
 *   <li>商户特定统计</li>
 * </ul>
 */
@Data
@Builder
public class BookingStatsResponse {

    // ============================================
    // 总体统计
    // ============================================

    /**
     * 预约总数（所有时间）。
     */
    private long totalBookings;

    /**
     * 活跃预约总数（PENDING + CONFIRMED）。
     */
    private long activeBookings;

    /**
     * 待处理预约总数。
     */
    private long pendingBookings;

    /**
     * 已确认预约总数。
     */
    private long confirmedBookings;

    /**
     * 已取消预约总数。
     */
    private long cancelledBookings;

    /**
     * 已完成预约总数。
     */
    private long completedBookings;

    // ============================================
    // 今日统计
    // ============================================

    /**
     * 今日创建的预约数。
     */
    private long todayBookings;

    /**
     * 今日活跃预约数。
     */
    private long todayActiveBookings;

    /**
     * 今日完成的预约数。
     */
    private long todayCompletedBookings;

    // ============================================
    // 日期范围统计
    // ============================================

    /**
     * 统计周期的开始日期。
     */
    private LocalDate startDate;

    /**
     * 统计周期的结束日期。
     */
    private LocalDate endDate;

    /**
     * 日期范围内的预约数。
     */
    private long periodBookings;

    /**
     * 每日预约明细（日期 -> 数量）。
     */
    private Map<LocalDate, Long> dailyBookings;

    // ============================================
    // 成功率指标
    // ============================================

    /**
     * 预约完成率（已完成 / 总数）。
     */
    private double completionRate;

    /**
     * 预约取消率（已取消 / 总数）。
     */
    private double cancellationRate;

    /**
     * 预约确认率（已确认 / 总数）。
     */
    private double confirmationRate;
}
