package org.example.appointment_system.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商户设置更新请求DTO。
 *
 * <p>包含商户账户的可配置设置。
 * 设置以 JSON 格式存储在数据库中以提供灵活性。</p>
 *
 * <h3>可用设置：</h3>
 * <ul>
 *   <li>sessionTimeout：会话超时时间，单位秒（默认：14400 = 4小时）</li>
 *   <li>notifications：启用/禁用邮件通知</li>
 *   <li>timezone：商户时区（例如："Asia/Shanghai"）</li>
 *   <li>bookingAdvanceDays：提前多少天可以预约</li>
 *   <li>cancelDeadline：预约开始前多少小时允许取消</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantSettingsRequest {

    /**
     * 会话超时时间，单位秒。
     * 默认值：14400（4小时）。
     * 最小值：1800（30分钟），最大值：86400（24小时）。
     */
    private Integer sessionTimeout;

    /**
     * 启用新预约的邮件通知。
     */
    private Boolean notificationsEnabled;

    /**
     * 向客户显示时间的商户时区。
     * 示例："Asia/Shanghai"、"America/New_York"。
     */
    private String timezone;

    /**
     * 提前多少天可以预约。
     * 默认值：30天。
     */
    private Integer bookingAdvanceDays;

    /**
     * 预约开始前多少小时仍允许取消。
     * 默认值：24小时。
     */
    private Integer cancelDeadlineHours;

    /**
     * 启用自动确认预约。
     * 如果为 true，预约将自动确认而非待确认状态。
     */
    private Boolean autoConfirmBookings;

    /**
     * 每个用户每天的最大预约数。
     * 0 或 null 表示无限制。
     */
    private Integer maxBookingsPerUserPerDay;
}
