package org.example.appointment_system.repository;

import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 商户实体Repository接口。
 *
 * <p>提供商户数据访问操作，包括：</p>
 * <ul>
 *   <li>标准CRUD操作（继承自JpaRepository）</li>
 *   <li>按用户ID查询（用于通过用户查找商户）</li>
 *   <li>按用户实体查询</li>
 *   <li>按用户ID检查是否存在</li>
 * </ul>
 */
@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {

    /**
     * 按关联用户的ID查询商户资料。
     *
     * <p>根据用户账户ID查找商户资料。</p>
     *
     * @param userId 要查询的用户ID
     * @return 包含商户资料的Optional（如果找到）
     */
    Optional<MerchantProfile> findByUserId(Long userId);

    /**
     * 按关联的用户实体查询商户资料。
     *
     * <p>直接使用User实体的替代方法。</p>
     *
     * @param user 要查询的用户实体
     * @return 包含商户资料的Optional（如果找到）
     */
    Optional<MerchantProfile> findByUser(User user);

    /**
     * 检查指定用户ID是否存在商户资料。
     *
     * <p>在创建新商户资料前验证用户是否已有商户资料。</p>
     *
     * @param userId 要检查的用户ID
     * @return 如果该用户存在资料返回true，否则返回false
     */
    boolean existsByUserId(Long userId);

    /**
     * 检查指定用户实体是否存在商户资料。
     *
     * @param user 要检查的用户实体
     * @return 如果该用户存在资料返回true，否则返回false
     */
    boolean existsByUser(User user);

    /**
     * 按商户名称查询商户资料。
     *
     * <p>用于按商户名称搜索商户。
     * 注意：商户名称不唯一，因此返回Optional。</p>
     *
     * @param businessName 要查询的商户名称
     * @return 包含第一个匹配的商户资料的Optional
     */
    Optional<MerchantProfile> findByBusinessName(String businessName);

    /**
     * 检查是否存在指定商户名称的商户资料。
     *
     * @param businessName 要检查的商户名称
     * @return 如果任何资料使用此名称返回true
     */
    boolean existsByBusinessName(String businessName);
}
