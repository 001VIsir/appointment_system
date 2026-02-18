package org.example.appointment_system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.config.CacheConfig;
import org.example.appointment_system.dto.request.MerchantProfileRequest;
import org.example.appointment_system.dto.request.MerchantSettingsRequest;
import org.example.appointment_system.dto.response.MerchantProfileResponse;
import org.example.appointment_system.dto.response.MerchantSettingsResponse;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.UserRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * 商家资料管理操作服务类。
 *
 * <p>处理商家资料的CRUD操作，包括：</p>
 * <ul>
 *   <li>为具有MERCHANT角色的用户创建商家资料</li>
 *   <li>更新商家资料信息</li>
 *   <li>获取商家资料数据</li>
 *   <li>管理商家设置（存储为JSON）</li>
 * </ul>
 *
 * <h3>安全性：</h3>
 * <p>所有操作要求当前用户具有MERCHANT角色。
 * 用户只能访问和修改自己的商家资料。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 为当前用户创建新的商家资料。
     *
     * <p>当前用户必须具有MERCHANT角色，且尚未拥有商家资料。</p>
     *
     * @param request 资料创建请求
     * @return 包含已创建资料的MerchantProfileResponse
     * @throws IllegalStateException 如果用户已有商家资料
     * @throws IllegalArgumentException 如果用户不是MERCHANT
     */
    @Transactional
    public MerchantProfileResponse createProfile(MerchantProfileRequest request) {
        User currentUser = getCurrentUserOrThrow();

        // 验证用户是商家
        if (currentUser.getRole() != UserRole.MERCHANT) {
            log.warn("User {} (role={}) attempted to create merchant profile",
                currentUser.getUsername(), currentUser.getRole());
            throw new IllegalArgumentException("Only users with MERCHANT role can create a merchant profile");
        }

        // 检查资料是否已存在
        if (merchantProfileRepository.existsByUserId(currentUser.getId())) {
            log.warn("User {} already has a merchant profile", currentUser.getUsername());
            throw new IllegalStateException("User already has a merchant profile");
        }

        // 创建新资料
        MerchantProfile profile = new MerchantProfile(
            currentUser,
            request.getBusinessName(),
            request.getDescription(),
            request.getPhone(),
            request.getAddress(),
            null // settings
        );

        MerchantProfile savedProfile = merchantProfileRepository.save(profile);
        log.info("Created merchant profile for user {}: id={}",
            currentUser.getUsername(), savedProfile.getId());

        return mapToResponse(savedProfile);
    }

    /**
     * 获取当前用户的商家资料。
     *
     * <p>结果会被缓存以提高性能。</p>
     *
     * @return 包含MerchantProfileResponse的Optional（如果找到）
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_MERCHANT_PROFILES, key = "#root.methodName + ':' + T(org.example.appointment_system.service.MerchantService).getCurrentUserId()")
    public Optional<MerchantProfileResponse> getCurrentMerchantProfile() {
        User currentUser = getCurrentUserOrThrow();

        return merchantProfileRepository.findByUserId(currentUser.getId())
            .map(this::mapToResponse);
    }

    /**
     * 获取当前用户ID用于缓存键的辅助方法。
     * 此方法在内部用于缓存键生成。
     *
     * @return 当前用户ID或null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    /**
     * 根据ID获取商家资料。
     *
     * <p>只允许访问当前用户自己的资料。</p>
     *
     * @param profileId 资料ID
     * @return 包含MerchantProfileResponse的Optional（如果找到且属于当前用户）
     */
    @Transactional(readOnly = true)
    public Optional<MerchantProfileResponse> getProfileById(Long profileId) {
        User currentUser = getCurrentUserOrThrow();

        return merchantProfileRepository.findById(profileId)
            .filter(profile -> profile.getUser().getId().equals(currentUser.getId()))
            .map(this::mapToResponse);
    }

    /**
     * 更新当前用户的商家资料。
     *
     * <p>更新后清除缓存以确保一致性。</p>
     *
     * @param request 资料更新请求
     * @return 包含已更新资料的MerchantProfileResponse
     * @throws IllegalArgumentException 如果用户没有商家资料
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MERCHANT_PROFILES, allEntries = true)
    public MerchantProfileResponse updateProfile(MerchantProfileRequest request) {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        // 更新字段
        profile.setBusinessName(request.getBusinessName());
        profile.setDescription(request.getDescription());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());

        MerchantProfile updatedProfile = merchantProfileRepository.save(profile);
        log.info("Updated merchant profile for user {}: id={}",
            currentUser.getUsername(), updatedProfile.getId());

        return mapToResponse(updatedProfile);
    }

    /**
     * 获取当前商家的设置。
     *
     * <p>结果会被缓存以提高性能。</p>
     *
     * @return 包含设置的MerchantSettingsResponse
     * @throws IllegalArgumentException 如果用户没有商家资料
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_MERCHANT_SETTINGS, key = "'settings:' + T(org.example.appointment_system.service.MerchantService).getCurrentUserId()")
    public MerchantSettingsResponse getSettings() {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        return parseSettings(profile.getSettings());
    }

    /**
     * 更新商家的设置。
     *
     * <p>将提供的设置与现有设置合并。
     * 更新后清除缓存以确保一致性。</p>
     *
     * @param request 设置更新请求
     * @return 包含已更新设置的MerchantSettingsResponse
     * @throws IllegalArgumentException 如果用户没有商家资料
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MERCHANT_SETTINGS, allEntries = true)
    public MerchantSettingsResponse updateSettings(MerchantSettingsRequest request) {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        // 解析现有设置或创建新映射
        @SuppressWarnings("unchecked")
        Map<String, Object> settingsMap = profile.getSettings() != null
            ? parseSettingsToMap(profile.getSettings())
            : new java.util.HashMap<>();

        // 从请求更新设置
        if (request.getSessionTimeout() != null) {
            settingsMap.put("sessionTimeout", request.getSessionTimeout());
        }
        if (request.getNotificationsEnabled() != null) {
            settingsMap.put("notificationsEnabled", request.getNotificationsEnabled());
        }
        if (request.getTimezone() != null) {
            settingsMap.put("timezone", request.getTimezone());
        }
        if (request.getBookingAdvanceDays() != null) {
            settingsMap.put("bookingAdvanceDays", request.getBookingAdvanceDays());
        }
        if (request.getCancelDeadlineHours() != null) {
            settingsMap.put("cancelDeadlineHours", request.getCancelDeadlineHours());
        }
        if (request.getAutoConfirmBookings() != null) {
            settingsMap.put("autoConfirmBookings", request.getAutoConfirmBookings());
        }
        if (request.getMaxBookingsPerUserPerDay() != null) {
            settingsMap.put("maxBookingsPerUserPerDay", request.getMaxBookingsPerUserPerDay());
        }

        // 保存更新的设置
        try {
            profile.setSettings(objectMapper.writeValueAsString(settingsMap));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize settings for user {}", currentUser.getUsername(), e);
            throw new RuntimeException("Failed to save settings", e);
        }

        MerchantProfile updatedProfile = merchantProfileRepository.save(profile);
        log.info("Updated settings for merchant user {}", currentUser.getUsername());

        return parseSettings(updatedProfile.getSettings());
    }

    /**
     * 删除当前用户的商家资料。
     *
     * <p>注意：这是硬删除。如果业务需求需要保留历史数据，
     * 请考虑实现软删除。删除后清除缓存。</p>
     *
     * @throws IllegalArgumentException 如果用户没有商家资料
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_MERCHANT_PROFILES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_MERCHANT_SETTINGS, allEntries = true)
    })
    public void deleteProfile() {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        merchantProfileRepository.delete(profile);
        log.info("Deleted merchant profile for user {}: id={}",
            currentUser.getUsername(), profile.getId());
    }

    /**
     * 检查当前用户是否有商家资料。
     *
     * @return 如果当前用户有商家资料返回true
     */
    @Transactional(readOnly = true)
    public boolean hasMerchantProfile() {
        User currentUser = getCurrentUserOrThrow();
        return merchantProfileRepository.existsByUserId(currentUser.getId());
    }

    /**
     * 获取当前商家的资料ID。
     *
     * @return 包含商家资料ID的Optional（如果找到）
     */
    @Transactional(readOnly = true)
    public Optional<Long> getCurrentMerchantId() {
        User currentUser = getCurrentUserOrThrow();
        return merchantProfileRepository.findByUserId(currentUser.getId())
                .map(MerchantProfile::getId);
    }

    /**
     * 获取当前已认证的用户。
     *
     * @return 包含当前用户的Optional
     */
    private Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findById(userDetails.getId());
        }

        return Optional.empty();
    }

    /**
     * 获取当前已认证的用户或抛出异常。
     *
     * @return 当前用户
     * @throws IllegalStateException 如果没有已认证的用户
     */
    private User getCurrentUserOrThrow() {
        return getCurrentUser()
            .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * 将MerchantProfile实体映射到响应DTO。
     *
     * @param profile 要映射的实体
     * @return 响应DTO
     */
    private MerchantProfileResponse mapToResponse(MerchantProfile profile) {
        return MerchantProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUser().getId())
            .username(profile.getUser().getUsername())
            .businessName(profile.getBusinessName())
            .description(profile.getDescription())
            .phone(profile.getPhone())
            .address(profile.getAddress())
            .settings(profile.getSettings())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }

    /**
     * 将JSON设置字符串解析为MerchantSettingsResponse。
     *
     * @param settingsJson JSON字符串
     * @return 设置响应
     */
    private MerchantSettingsResponse parseSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return MerchantSettingsResponse.builder().build();
        }

        try {
            return objectMapper.readValue(settingsJson, MerchantSettingsResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse settings JSON: {}", settingsJson, e);
            return MerchantSettingsResponse.builder().build();
        }
    }

    /**
     * 将JSON设置字符串解析为Map。
     *
     * @param settingsJson JSON字符串
     * @return 设置Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSettingsToMap(String settingsJson) {
        try {
            return objectMapper.readValue(settingsJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse settings JSON to map: {}", settingsJson, e);
            return new java.util.HashMap<>();
        }
    }
}
