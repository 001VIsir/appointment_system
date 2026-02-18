package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 预约创建请求DTO。
 *
 * <p>包含创建新预约所需的必要信息。</p>
 *
 * <h3>校验规则：</h3>
 * <ul>
 *   <li>slotId：必填，必须是有效的时段ID</li>
 *   <li>remark：可选，最多500个字符</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    /**
     * 要预约的时段ID。
     * 必填字段。
     */
    private Long slotId;

    /**
     * 用户可选的备注或说明。
     * 可包含对商户的特殊要求或备注。
     * 最多500个字符。
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
