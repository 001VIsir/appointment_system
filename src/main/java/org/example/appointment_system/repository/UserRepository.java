package org.example.appointment_system.repository;

import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户实体Repository接口。
 *
 * <p>提供用户数据访问操作，包括：</p>
 * <ul>
 *   <li>标准CRUD操作（继承自JpaRepository）</li>
 *   <li>按用户名查询（用于认证）</li>
 *   <li>按邮箱查询（用于唯一性检查）</li>
 *   <li>按角色查询（用于用户管理）</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 按用户名查询用户。
     *
     * <p>认证时用于加载用户详情。</p>
     *
     * @param username 要查询的用户名
     * @return 包含用户的Optional（如果找到）
     */
    Optional<User> findByUsername(String username);

    /**
     * 按邮箱地址查询用户。
     *
     * <p>注册时用于邮箱唯一性验证。</p>
     *
     * @param email 要查询的邮箱地址
     * @return 包含用户的Optional（如果找到）
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否已存在。
     *
     * @param username 要检查的用户名
     * @return 如果用户名存在返回true，否则返回false
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在。
     *
     * @param email 要检查的邮箱
     * @return 如果邮箱存在返回true，否则返回false
     */
    boolean existsByEmail(String email);

    /**
     * 按角色查询所有用户。
     *
     * <p>管理员用于按类型列出用户。</p>
     *
     * @param role 要筛选的角色
     * @return 具有指定角色的用户列表
     */
    List<User> findByRole(UserRole role);

    /**
     * 查询所有已启用的用户。
     *
     * @return 已启用的用户列表
     */
    List<User> findByEnabledTrue();

    /**
     * 查询所有已禁用的用户。
     *
     * @return 已禁用的用户列表
     */
    List<User> findByEnabledFalse();

    /**
     * 按角色和启用状态查询用户。
     *
     * @param role    要筛选的角色
     * @param enabled 启用状态
     * @return 匹配条件的用户列表
     */
    List<User> findByRoleAndEnabled(UserRole role, boolean enabled);

    /**
     * 按角色统计用户数量。
     *
     * @param role 要统计的角色
     * @return 具有指定角色的用户数量
     */
    long countByRole(UserRole role);
}
