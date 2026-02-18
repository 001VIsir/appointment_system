package org.example.appointment_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商户设置响应数据传输对象。
 *
 * <p>包含商户账户的所有可配置设置及其当前值。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantSettingsResponse {

    /**
     * 会话超时时间（秒）。
     */
    private Integer sessionTimeout;

    /**
     * 是否启用新预约的邮件通知。
     */
    private Boolean notificationsEnabled;

    /**
     * 商家时区。
     */
    private String timezone;

    /**
     * 允许提前预约的天数。
     */
    private Integer bookingAdvanceDays;

    /**
     * 允许取消预约的小时数（提前预约时间）。
     */
    private Integer cancelDeadlineHours;

    /**
     * 是否启用自动确认预约。
     */
    private Boolean autoConfirmBookings;

    /**
     * 每个用户每天的最大预约数。
     */
    private Integer maxBookingsPerUserPerDay;
}
