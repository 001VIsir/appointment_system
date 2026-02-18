package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约响应数据传输对象。
 *
 * <p>包含API响应的所有预约信息，包括相关的任务和服务详情。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    /**
     * 预约的唯一标识符。
     */
    private Long id;

    /**
     * 预约用户的ID。
     */
    private Long userId;

    /**
     * 预约用户的用户名。
     */
    private String username;

    /**
     * 预约时段的ID。
     */
    private Long slotId;

    /**
     * 预约任务的ID。
     */
    private Long taskId;

    /**
     * 预约任务的标题。
     */
    private String taskTitle;

    /**
     * 预约日期。
     */
    private LocalDate taskDate;

    /**
     * 已预约时段的开始时间。
     */
    private LocalTime startTime;

    /**
     * 已预约时段的结束时间。
     */
    private LocalTime endTime;

    /**
     * 服务项目的ID。
     */
    private Long serviceId;

    /**
     * 服务名称。
     */
    private String serviceName;

    /**
     * 商户的ID。
     */
    private Long merchantId;

    /**
     * 商户的商家名称。
     */
    private String merchantBusinessName;

    /**
     * 预约的当前状态。
     */
    private BookingStatus status;

    /**
     * 状态的显示名称。
     */
    private String statusDisplayName;

    /**
     * 用户的可选备注。
     */
    private String remark;

    /**
     * 乐观锁版本号。
     */
    private Long version;

    /**
     * 预约创建的时间戳。
     */
    private LocalDateTime createdAt;

    /**
     * 预约最后更新的时间戳。
     */
    private LocalDateTime updatedAt;
}
