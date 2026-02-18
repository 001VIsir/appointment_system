package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.UserRole;

import java.time.LocalDateTime;

/**
 * 用户响应数据传输对象。
 *
 * <p>不包含敏感信息（如密码）。用于包含用户信息的API响应。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /**
     * 用户的唯一标识符。
     */
    private Long id;

    /**
     * 账户的用户名。
     */
    private String username;

    /**
     * 用户的电子邮箱地址。
     */
    private String email;

    /**
     * 分配给用户的角色。
     */
    private UserRole role;

    /**
     * 账户是否已启用。
     */
    private boolean enabled;

    /**
     * 账户创建的时间戳。
     */
    private LocalDateTime createdAt;

    /**
     * 账户最后更新的时间戳。
     */
    private LocalDateTime updatedAt;
}
