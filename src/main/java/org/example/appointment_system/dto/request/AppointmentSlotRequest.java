package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 预约时段创建请求DTO。
 *
 * <p>包含在任务中创建时间段所需的所有字段。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSlotRequest {

    /**
     * 时段的开始时间。
     * 必填。
     */
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    /**
     * 时段的结束时间。
     * 必填。
     */
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    /**
     * 该时段允许的最大预约数。
     * 必填，最小值为1。
     */
    @NotNull(message = "容量不能为空")
    @Min(value = 1, message = "容量至少为1")
    private Integer capacity;
}
