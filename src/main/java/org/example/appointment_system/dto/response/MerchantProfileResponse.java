package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 商户资料响应数据传输对象。
 *
 * <p>包含所有商户资料信息，包括设置。用于包含商户资料数据的API响应。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfileResponse {

    /**
     * 商户资料的唯一标识符。
     */
    private Long id;

    /**
     * 关联用户账户的ID。
     */
    private Long userId;

    /**
     * 关联用户账户的用户名。
     */
    private String username;

    /**
     * 商户的商家名称。
     */
    private String businessName;

    /**
     * 商家的可选描述。
     */
    private String description;

    /**
     * 联系电话号码。
     */
    private String phone;

    /**
     * 商家地址。
     */
    private String address;

    /**
     * 商户设置，存储为JSON字符串。
     */
    private String settings;

    /**
     * 资料创建的时间戳。
     */
    private LocalDateTime createdAt;

    /**
     * 资料最后更新的时间戳。
     */
    private LocalDateTime updatedAt;
}
