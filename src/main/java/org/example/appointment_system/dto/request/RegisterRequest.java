package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.UserRole;

/**
 * 用户注册请求DTO。
 *
 * <p>包含创建新用户账户所需的所有字段。</p>
 *
 * <h3>校验规则：</h3>
 * <ul>
 *   <li>用户名：3-50个字符，可包含字母数字和下划线</li>
 *   <li>密码：最少6个字符</li>
 *   <li>邮箱：有效的邮箱格式</li>
 *   <li>角色：可选，默认为 USER</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    /**
     * 账户的用户名。
     * 必须在系统中唯一。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 账户的密码。
     * 存储前会使用 BCrypt 进行哈希处理。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    private String password;

    /**
     * 账户的邮箱地址。
     * 必须在系统中唯一。
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式无效")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /**
     * 新用户的角色。
     * 可选，如果不指定默认为 USER。
     * 只有 ADMIN 可以创建 MERCHANT 或 ADMIN 账户。
     */
    private UserRole role;
}
