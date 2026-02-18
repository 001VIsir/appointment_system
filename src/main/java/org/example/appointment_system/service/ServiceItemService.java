package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.config.CacheConfig;
import org.example.appointment_system.dto.request.ServiceItemRequest;
import org.example.appointment_system.dto.response.ServiceItemResponse;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.repository.UserRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 服务项目管理工作服务类。
 *
 * <p>处理服务项目的CRUD操作，包括：</p>
 * <ul>
 *   <li>为商家创建服务项目</li>
 *   <li>更新服务项目信息</li>
 *   <li>软删除服务项目（设置active=false）</li>
 *   <li>获取商家的服务项目</li>
 * </ul>
 *
 * <h3>安全性：</h3>
 * <p>所有操作要求当前用户具有MERCHANT角色和现有的
 * 商家资料。用户只能访问和修改自己的服务项目。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final UserRepository userRepository;

    /**
     * 为当前商家创建新的服务项目。
     *
     * <p>当前用户必须拥有商家资料。服务名称必须在
     * 商家的目录中唯一。创建后清除缓存以确保一致性。</p>
     *
     * @param request 服务项目创建请求
     * @return 包含已创建服务项目的ServiceItemResponse
     * @throws IllegalStateException 如果用户没有商家资料
     * @throws IllegalArgumentException 如果同名服务已存在
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SERVICE_ITEM_LISTS, allEntries = true)
    public ServiceItemResponse createServiceItem(ServiceItemRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        // 检查服务名称是否重复
        if (serviceItemRepository.existsByMerchantAndName(merchantProfile, request.getName())) {
            log.warn("Service item with name '{}' already exists for merchant {}",
                request.getName(), merchantProfile.getId());
            throw new IllegalArgumentException("A service item with this name already exists");
        }

        ServiceItem serviceItem = new ServiceItem(
            merchantProfile,
            request.getName(),
            request.getDescription(),
            request.getCategory(),
            request.getDuration(),
            request.getPrice(),
            request.getActive() != null ? request.getActive() : true
        );

        ServiceItem savedItem = serviceItemRepository.save(serviceItem);
        log.info("Created service item '{}' for merchant {}: id={}",
            savedItem.getName(), merchantProfile.getId(), savedItem.getId());

        return mapToResponse(savedItem);
    }

    /**
     * 更新现有的服务项目。
     *
     * <p>只有拥有该服务项目的商家才能更新它。
     * 如果要更改名称，新名称不能与现有服务名称冲突。
     * 更新后清除缓存以确保一致性。</p>
     *
     * @param serviceId 要更新的服务项目ID
     * @param request 更新请求
     * @return 包含已更新服务项目的ServiceItemResponse
     * @throws IllegalArgumentException 如果服务未找到或不属于当前商家
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SERVICE_ITEM_LISTS, allEntries = true)
    public ServiceItemResponse updateServiceItem(Long serviceId, ServiceItemRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        ServiceItem serviceItem = serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service item not found or not owned by current merchant"));

        // 如果名称被更改，检查是否重复
        if (!serviceItem.getName().equals(request.getName()) &&
            serviceItemRepository.existsByMerchantAndName(merchantProfile, request.getName())) {
            log.warn("Cannot rename service to '{}' - name already exists for merchant {}",
                request.getName(), merchantProfile.getId());
            throw new IllegalArgumentException("A service item with this name already exists");
        }

        // 更新字段
        serviceItem.setName(request.getName());
        serviceItem.setDescription(request.getDescription());
        serviceItem.setCategory(request.getCategory());
        serviceItem.setDuration(request.getDuration());
        serviceItem.setPrice(request.getPrice());
        if (request.getActive() != null) {
            serviceItem.setActive(request.getActive());
        }

        ServiceItem updatedItem = serviceItemRepository.save(serviceItem);
        log.info("Updated service item {} for merchant {}", serviceId, merchantProfile.getId());

        return mapToResponse(updatedItem);
    }

    /**
     * 通过设置active=false软删除服务项目。
     *
     * <p>只有拥有该服务项目的商家才能删除它。
     * 这是软删除——记录保留在数据库中，
     * 但从活动列表中隐藏。删除后清除缓存以确保一致性。</p>
     *
     * @param serviceId 要删除的服务项目ID
     * @throws IllegalArgumentException 如果服务未找到或不属于当前商家
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_SERVICE_ITEMS, key = "#serviceId"),
        @CacheEvict(value = CacheConfig.CACHE_SERVICE_ITEM_LISTS, allEntries = true)
    })
    public void deleteServiceItem(Long serviceId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        ServiceItem serviceItem = serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service item not found or not owned by current merchant"));

        // 软删除：设置active为false
        serviceItem.setActive(false);
        serviceItemRepository.save(serviceItem);
        log.info("Soft deleted service item {} for merchant {}", serviceId, merchantProfile.getId());
    }

    /**
     * 重新激活先前软删除的服务项目。
     *
     * <p>重新激活后清除缓存以确保一致性。</p>
     *
     * @param serviceId 要重新激活的服务项目ID
     * @return 包含已重新激活服务项目的ServiceItemResponse
     * @throws IllegalArgumentException 如果服务未找到或不属于当前商家
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_SERVICE_ITEMS, key = "#serviceId"),
        @CacheEvict(value = CacheConfig.CACHE_SERVICE_ITEM_LISTS, allEntries = true)
    })
    public ServiceItemResponse reactivateServiceItem(Long serviceId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        ServiceItem serviceItem = serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service item not found or not owned by current merchant"));

        serviceItem.setActive(true);
        ServiceItem reactivatedItem = serviceItemRepository.save(serviceItem);
        log.info("Reactivated service item {} for merchant {}", serviceId, merchantProfile.getId());

        return mapToResponse(reactivatedItem);
    }

    /**
     * 根据ID获取服务项目。
     *
     * <p>只返回属于当前商家的服务项目。
     * 结果会被缓存以提高性能。</p>
     *
     * @param serviceId 服务项目ID
     * @return 包含ServiceItemResponse的Optional（如果找到且属于当前商家）
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_SERVICE_ITEMS, key = "#serviceId", unless = "#result == null || !#result.isPresent()")
    public Optional<ServiceItemResponse> getServiceItemById(Long serviceId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .map(this::mapToResponse);
    }

    /**
     * 获取当前商家的所有服务项目。
     *
     * <p>返回活动和非活动的服务项目。
     * 结果会被缓存以提高性能。</p>
     *
     * @return 商户所有服务的ServiceItemResponse列表
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_SERVICE_ITEM_LISTS, key = "'all:' + T(org.example.appointment_system.service.ServiceItemService).getCurrentMerchantId()")
    public List<ServiceItemResponse> getAllServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchant(merchantProfile).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * 获取当前商家ID用于缓存键的辅助方法。
     * 此方法在内部用于缓存键生成。
     *
     * @return 当前商家资料ID或null
     */
    public static Long getCurrentMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            // 这需要查询数据库，但对于缓存键我们可以使用用户ID
            return userDetails.getId();
        }
        return null;
    }

    /**
     * 获取当前商家的所有活动服务项目。
     *
     * <p>只返回可用于预约的服务项目。
     * 结果会被缓存以提高性能。</p>
     *
     * @return 活动服务的ServiceItemResponse列表
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_SERVICE_ITEM_LISTS, key = "'active:' + T(org.example.appointment_system.service.ServiceItemService).getCurrentMerchantId()")
    public List<ServiceItemResponse> getActiveServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchantAndActiveTrue(merchantProfile).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * 获取当前商家的所有非活动（软删除）服务项目。
     *
     * <p>用于管理/重新激活先前删除的服务。</p>
     *
     * @return 非活动服务的ServiceItemResponse列表
     */
    @Transactional(readOnly = true)
    public List<ServiceItemResponse> getInactiveServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchantAndActiveFalse(merchantProfile).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * 统计当前商家的服务项目总数。
     *
     * @return 所有服务项目的数量
     */
    @Transactional(readOnly = true)
    public long countAllServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();
        return serviceItemRepository.countByMerchant(merchantProfile);
    }

    /**
     * 统计当前商家的活动服务项目数量。
     *
     * @return 活动服务项目的数量
     */
    @Transactional(readOnly = true)
    public long countActiveServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();
        return serviceItemRepository.countByMerchantAndActiveTrue(merchantProfile);
    }

    /**
     * 检查当前商家是否存在具有给定名称的服务项目。
     *
     * @param name 要检查的服务名称
     * @return 如果存在此名称的服务返回true
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();
        return serviceItemRepository.existsByMerchantAndName(merchantProfile, name);
    }

    /**
     * 获取当前已认证用户的商家资料。
     *
     * @return 包含MerchantProfile的Optional
     */
    private Optional<MerchantProfile> getCurrentMerchantProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return merchantProfileRepository.findByUserId(userDetails.getId());
        }

        return Optional.empty();
    }

    /**
     * 获取当前已认证用户的商家资料或抛出异常。
     *
     * @return MerchantProfile
     * @throws IllegalStateException 如果没有已认证用户或没有商家资料
     */
    private MerchantProfile getCurrentMerchantProfileOrThrow() {
        return getCurrentMerchantProfile()
            .orElseThrow(() -> new IllegalStateException(
                "No merchant profile found for current user. Please create a merchant profile first."));
    }

    /**
     * 将ServiceItem实体映射到响应DTO。
     *
     * @param serviceItem 要映射的实体
     * @return 响应DTO
     */
    private ServiceItemResponse mapToResponse(ServiceItem serviceItem) {
        return ServiceItemResponse.builder()
            .id(serviceItem.getId())
            .merchantId(serviceItem.getMerchant().getId())
            .merchantBusinessName(serviceItem.getMerchant().getBusinessName())
            .name(serviceItem.getName())
            .description(serviceItem.getDescription())
            .category(serviceItem.getCategory())
            .categoryDisplayName(serviceItem.getCategory().getDisplayName())
            .duration(serviceItem.getDuration())
            .price(serviceItem.getPrice())
            .active(serviceItem.getActive())
            .createdAt(serviceItem.getCreatedAt())
            .updatedAt(serviceItem.getUpdatedAt())
            .build();
    }
}
