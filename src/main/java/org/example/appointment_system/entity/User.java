package org.example.appointment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.UserRole;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体，代表系统中的所有用户账户。
 *
 * <p>此实体存储三种用户的信息：</p>
 * <ul>
 *   <li>管理员(ADMIN) - 完整系统访问权限</li>
 *   <li>商家(MERCHANT) - 管理预约的企业主</li>
 *   <li>用户(USER) - 预约服务的终端用户</li>
 * </ul>
 *
 * <p>实体使用自动生成的ID和时间戳用于审计目的。</p>
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_role", columnList = "role")
})
@Getter
@Setter
@NoArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户的唯一标识符。
     * 由数据库自动生成。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用于认证的唯一用户名。
     * 必须在3-50个字符之间。
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 使用BCrypt哈希的密码。
     * 绝不以明文存储。
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 唯一的电子邮件地址。
     * 用于通知和账户恢复。
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 决定访问权限的用户角色。
     * 默认为USER角色。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * 指示用户账户是否启用的标志。
     * 禁用的账户无法进行身份验证。
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * 用户创建时的时间戳。
     * 持久化时自动设置。
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 用户最后更新时间的时间戳。
     * 修改时自动更新。
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 预持久化回调，用于设置时间戳。
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.role == null) {
            this.role = UserRole.USER;
        }
    }

    /**
     * 预更新回调，用于更新修改时间戳。
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 创建用户的便捷构造函数。
     *
     * @param username 用户名
     * @param password 哈希后的密码
     * @param email    电子邮件地址
     * @param role     用户角色
     */
    public User(String username, String password, String email, UserRole role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
}
