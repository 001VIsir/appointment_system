package org.example.appointment_system.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 用户统计响应数据传输对象。
 *
 * <p>包含各种用户指标和统计数据：</p>
 * <ul>
 *   <li>按角色统计的用户总数</li>
 *   <li>新用户注册统计</li>
 *   <li>用户活跃度统计</li>
 * </ul>
 */
@Data
@Builder
public class UserStatsResponse {

    // ============================================
    // 用户总体统计
    // ============================================

    /**
     * 用户总数。
     */
    private long totalUsers;

    /**
     * 已启用的用户总数。
     */
    private long enabledUsers;

    /**
     * 已禁用的用户总数。
     */
    private long disabledUsers;

    // ============================================
    // 基于角色的统计
    // ============================================

    /**
     * 拥有ADMIN角色的用户数量。
     */
    private long adminCount;

    /**
     * 拥有MERCHANT角色的用户数量。
     */
    private long merchantCount;

    /**
     * 拥有USER角色的用户数量。
     */
    private long userCount;

    // ============================================
    // 注册统计
    // ============================================

    /**
     * 今日注册的新用户数。
     */
    private long todayNewUsers;

    /**
     * 本周注册的新用户数。
     */
    private long weekNewUsers;

    /**
     * 本月注册的新用户数。
     */
    private long monthNewUsers;

    // ============================================
    // 活跃度统计
    // ============================================

    /**
     * 至少预约过一次的用户数量。
     */
    private long activeUsersWithBookings;

    /**
     * 至少有一个服务项目的商户数量。
     */
    private long activeMerchantsWithServices;

    /**
     * 每用户平均预约数。
     */
    private double averageBookingsPerUser;
}
