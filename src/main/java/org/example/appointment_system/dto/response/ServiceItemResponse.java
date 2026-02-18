package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务项目响应数据传输对象。
 *
 * <p>包含API响应的所有服务项目信息。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemResponse {

    /**
     * 服务项目的唯一标识符。
     */
    private Long id;

    /**
     * 拥有此服务的商户资料ID。
     */
    private Long merchantId;

    /**
     * 商户的商家名称。
     */
    private String merchantBusinessName;

    /**
     * 服务名称。
     */
    private String name;

    /**
     * 服务的可选描述。
     */
    private String description;

    /**
     * 服务类别。
     */
    private ServiceCategory category;

    /**
     * 供前端显示的类别名称。
     */
    private String categoryDisplayName;

    /**
     * 服务时长（分钟）。
     */
    private Integer duration;

    /**
     * 服务价格。
     */
    private BigDecimal price;

    /**
     * 服务是否启用并可预约。
     */
    private Boolean active;

    /**
     * 服务创建的时间戳。
     */
    private LocalDateTime createdAt;

    /**
     * 服务最后更新的时间戳。
     */
    private LocalDateTime updatedAt;
}
