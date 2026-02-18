package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 预约任务创建/更新请求DTO。
 *
 * <p>包含创建或更新预约任务所需的所有字段。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentTaskRequest {

    /**
     * 该任务所属的服务项目ID。
     * 创建任务时必填。
     */
    @NotNull(message = "服务ID不能为空")
    private Long serviceId;

    /**
     * 预约任务的标题。
     * 必填，2-100个字符。
     */
    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度必须在2到100个字符之间")
    private String title;

    /**
     * 预约任务的可选描述。
     * 最多1000个字符。
     */
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    /**
     * 预约任务的日期。
     * 必填，对于新任务必须是今天或未来日期。
     */
    @NotNull(message = "任务日期不能为空")
    private LocalDate taskDate;

    /**
     * 该任务的最大预约总数。
     * 必填，最小值为1。
     */
    @NotNull(message = "总容量不能为空")
    @Min(value = 1, message = "总容量至少为1")
    private Integer totalCapacity;

    /**
     * 任务是否启用。
     * 可选，默认为 true。
     */
    private Boolean active;
}
