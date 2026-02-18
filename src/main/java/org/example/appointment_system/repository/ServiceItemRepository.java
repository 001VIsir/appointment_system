package org.example.appointment_system.repository;

import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务项目实体Repository接口。
 *
 * <p>提供服务项目数据访问操作，包括：</p>
 * <ul>
 *   <li>标准CRUD操作（继承自JpaRepository）</li>
 *   <li>按商户资料查询</li>
 *   <li>按类别和启用状态查询</li>
 *   <li>按商户和名称检查是否存在</li>
 * </ul>
 */
@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    /**
     * 查询指定商户的所有服务项目。
     *
     * <p>返回商户的所有服务项目，包括启用和禁用的。</p>
     *
     * @param merchant 要查询的商户资料
     * @return 该商户的服务项目列表
     */
    List<ServiceItem> findByMerchant(MerchantProfile merchant);

    /**
     * 按商户ID查询所有服务项目。
     *
     * <p>直接使用商户ID的便捷方法。</p>
     *
     * @param merchantId 要查询的商户ID
     * @return 该商户的服务项目列表
     */
    List<ServiceItem> findByMerchantId(Long merchantId);

    /**
     * 查询指定商户的所有启用状态的服务项目。
     *
     * <p>用于向客户展示可用服务。</p>
     *
     * @param merchant 要查询的商户资料
     * @return 该商户的启用状态服务项目列表
     */
    List<ServiceItem> findByMerchantAndActiveTrue(MerchantProfile merchant);

    /**
     * 按商户ID查询所有启用状态的服务项目。
     *
     * @param merchantId 要查询的商户ID
     * @return 该商户的启用状态服务项目列表
     */
    List<ServiceItem> findByMerchantIdAndActiveTrue(Long merchantId);

    /**
     * 按ID和商户查询服务项目。
     *
     * <p>用于在执行操作前验证服务是否属于指定商户。</p>
     *
     * @param id       服务项目ID
     * @param merchant 商户资料
     * @return 包含服务项目的Optional（如果找到且属于该商户）
     */
    Optional<ServiceItem> findByIdAndMerchant(Long id, MerchantProfile merchant);

    /**
     * 按ID和商户ID查询服务项目。
     *
     * @param id         服务项目ID
     * @param merchantId 商户ID
     * @return 包含服务项目的Optional（如果找到）
     */
    Optional<ServiceItem> findByIdAndMerchantId(Long id, Long merchantId);

    /**
     * 按类别查询所有服务项目。
     *
     * <p>用于跨商户按类别筛选服务。</p>
     *
     * @param category 要筛选的服务类别
     * @return 指定类别的服务项目列表
     */
    List<ServiceItem> findByCategory(ServiceCategory category);

    /**
     * 按类别查询所有启用状态的服务项目。
     *
     * @param category 要筛选的服务类别
     * @return 指定类别的启用状态服务项目列表
     */
    List<ServiceItem> findByCategoryAndActiveTrue(ServiceCategory category);

    /**
     * 按商户和类别查询所有启用状态的服务项目。
     *
     * @param merchant 商户资料
     * @param category 服务类别
     * @return 匹配条件的启用状态服务项目列表
     */
    List<ServiceItem> findByMerchantAndCategoryAndActiveTrue(MerchantProfile merchant, ServiceCategory category);

    /**
     * 检查指定商户是否存在指定名称的服务项目。
     *
     * <p>用于防止商户目录中出现重复的服务名称。</p>
     *
     * @param merchant 商户资料
     * @param name     要检查的服务名称
     * @return 如果该商户存在此名称的服务返回true
     */
    boolean existsByMerchantAndName(MerchantProfile merchant, String name);

    /**
     * 按商户ID检查是否存在指定名称的服务项目。
     *
     * @param merchantId 商户ID
     * @param name       要检查的服务名称
     * @return 如果该商户存在此名称的服务返回true
     */
    boolean existsByMerchantIdAndName(Long merchantId, String name);

    /**
     * 统计指定商户的所有服务项目数量。
     *
     * @param merchant 商户资料
     * @return 服务项目数量
     */
    long countByMerchant(MerchantProfile merchant);

    /**
     * 统计指定商户的启用状态服务项目数量。
     *
     * @param merchant 商户资料
     * @return 启用状态服务项目数量
     */
    long countByMerchantAndActiveTrue(MerchantProfile merchant);

    /**
     * 查询指定商户的所有禁用状态的服务项目。
     *
     * <p>用于商户管理/重新启用被禁用的服务。</p>
     *
     * @param merchant 商户资料
     * @return 禁用状态服务项目列表
     */
    List<ServiceItem> findByMerchantAndActiveFalse(MerchantProfile merchant);
}
