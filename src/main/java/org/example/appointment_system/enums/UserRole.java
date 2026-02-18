package org.example.appointment_system.enums;

/**
 * 用户角色枚举，用于基于角色的访问控制。
 *
 * <p>定义预约系统中的三种用户类型：</p>
 * <ul>
 *   <li>{@link #ADMIN} - 拥有完全访问权限的系统管理员</li>
 *   <li>{@link #MERCHANT} - 创建预约任务的商家</li>
 *   <li>{@link #USER} - 预约服务的终端用户</li>
 * </ul>
 */
public enum UserRole {

    /**
     * 管理员角色，拥有完整的系统访问权限。
     * 可以管理所有用户、商家和系统设置。
     */
    ADMIN("ROLE_ADMIN", "Administrator"),

    /**
     * 商家角色，面向企业主。
     * 可以创建服务项目、预约任务并管理其预约。
     */
    MERCHANT("ROLE_MERCHANT", "Merchant"),

    /**
     * 标准用户角色，用于预约预订。
     * 可以查看可用任务并创建预约。
     */
    USER("ROLE_USER", "User");

    private final String authority;
    private final String displayName;

    UserRole(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }

    /**
     * 获取Spring Security权限名称。
     *
     * @return 权限字符串（例如："ROLE_ADMIN"）
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * 获取人类可读的显示名称。
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
