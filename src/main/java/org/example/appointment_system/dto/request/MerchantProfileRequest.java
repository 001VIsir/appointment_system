package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商户资料创建/更新请求DTO。
 *
 * <p>包含商户资料上所有可以设置或修改的字段。</p>
 *
 * <h3>校验规则：</h3>
 * <ul>
 *   <li>商户名称：必填，2-100个字符</li>
 *   <li>描述：可选，最多1000个字符</li>
 *   <li>电话：可选，最多20个字符，有效的电话格式</li>
 *   <li>地址：可选，最多255个字符</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfileRequest {

    /**
     * 商户的商家名称。
     * 必填字段，展示给客户。
     */
    @NotBlank(message = "商户名称不能为空")
    @Size(min = 2, max = 100, message = "商户名称长度必须在2到100个字符之间")
    private String businessName;

    /**
     * 商户的可选描述。
     * 可用于描述所提供的服务。
     */
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    /**
     * 联系电话。
     * 可选，但建议填写以便客户联系。
     */
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]*$", message = "电话号码格式无效")
    private String phone;

    /**
     * 商户地址。
     * 可选，适用于实体店铺。
     */
    @Size(max = 255, message = "地址长度不能超过255个字符")
    private String address;
}
