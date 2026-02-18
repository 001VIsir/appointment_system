package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户登录请求DTO。
 *
 * <p>包含用户认证所需的凭证信息。</p>
 *
 * <h3>校验规则：</h3>
 * <ul>
 *   <li>用户名：必填，3-50个字符</li>
 *   <li>密码：必填，最少6个字符</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    /**
     * 用于认证的用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间")
    private String username;

    /**
     * 用于认证的密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    private String password;
}
