package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约任务响应数据传输对象。
 *
 * <p>包含完整的任务信息，包括相关的服务详情。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTaskResponse {

    /**
     * 任务的唯一标识符。
     */
    private Long id;

    /**
     * 关联服务项目的ID。
     */
    private Long serviceId;

    /**
     * 关联服务的名称。
     */
    private String serviceName;

    /**
     * 拥有此任务的商户ID。
     */
    private Long merchantId;

    /**
     * 商户的商家名称。
     */
    private String merchantBusinessName;

    /**
     * 任务标题。
     */
    private String title;

    /**
     * 任务描述。
     */
    private String description;

    /**
     * 任务日期。
     */
    private LocalDate taskDate;

    /**
     * 此任务的最大预约总数。
     */
    private Integer totalCapacity;

    /**
     * 此任务中的时段总数。
     */
    private Integer slotCount;

    /**
     * 所有时段的容量总和。
     */
    private Integer totalSlotCapacity;

    /**
     * 所有时段的已预约总数。
     */
    private Integer totalBookedCount;

    /**
     * 任务是否启用。
     */
    private Boolean active;

    /**
     * 任务创建的时间戳。
     */
    private LocalDateTime createdAt;

    /**
     * 任务最后更新的时间戳。
     */
    private LocalDateTime updatedAt;
}
