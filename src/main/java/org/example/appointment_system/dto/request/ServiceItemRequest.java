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
import org.example.appointment_system.enums.ServiceCategory;

import java.math.BigDecimal;

/**
 * 服务项目创建/更新请求DTO。
 *
 * <p>包含服务项目上所有可以设置或修改的字段。</p>
 *
 * <h3>校验规则：</h3>
 * <ul>
 *   <li>名称：必填，2-100个字符</li>
 *   <li>描述：可选，最多1000个字符</li>
 *   <li>类别：必填，必须是有效的 ServiceCategory</li>
 *   <li>时长：必填，最少5分钟</li>
 *   <li>价格：必填，最小值为0</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemRequest {

    /**
     * 服务的名称。
     * 必填字段，展示给客户。
     */
    @NotBlank(message = "服务名称不能为空")
    @Size(min = 2, max = 100, message = "服务名称长度必须在2到100个字符之间")
    private String name;

    /**
     * 服务的可选描述。
     * 可用于描述服务包含的内容。
     */
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    /**
     * 服务的类别。
     * 用于分类和筛选。
     */
    @NotNull(message = "服务类别不能为空")
    private ServiceCategory category;

    /**
     * 服务的时长，单位分钟。
     * 最少5分钟以确保预约有意义。
     */
    @NotNull(message = "时长不能为空")
    @Min(value = 5, message = "时长至少为5分钟")
    private Integer duration;

    /**
     * 服务的价格。
     * 可以为0表示免费服务。
     */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal price;

    /**
     * 服务是否启用并可用于预约。
     * 新服务默认为 true。
     */
    private Boolean active;
}
