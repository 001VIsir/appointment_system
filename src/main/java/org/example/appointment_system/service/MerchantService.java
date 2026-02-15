package org.example.appointment_system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service for merchant profile management operations.
 *
 * <p>Handles CRUD operations for merchant profiles including:</p>
 * <ul>
 *   <li>Creating merchant profiles for MERCHANT role users</li>
 *   <li>Updating merchant profile information</li>
 *   <li>Retrieving merchant profile data</li>
 *   <li>Managing merchant settings (stored as JSON)</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>All operations require the current user to have MERCHANT role.
 * Users can only access and modify their own merchant profile.</p>
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
     * Create a new merchant profile for the current user.
     *
     * <p>The current user must have MERCHANT role and must not already
     * have a merchant profile.</p>
     *
     * @param request the profile creation request
     * @return MerchantProfileResponse containing the created profile
     * @throws IllegalStateException if user already has a merchant profile
     * @throws IllegalArgumentException if user is not a MERCHANT
     */
    @Transactional
    public MerchantProfileResponse createProfile(MerchantProfileRequest request) {
        User currentUser = getCurrentUserOrThrow();

        // Validate user is a merchant
        if (currentUser.getRole() != UserRole.MERCHANT) {
            log.warn("User {} (role={}) attempted to create merchant profile",
                currentUser.getUsername(), currentUser.getRole());
            throw new IllegalArgumentException("Only users with MERCHANT role can create a merchant profile");
        }

        // Check if profile already exists
        if (merchantProfileRepository.existsByUserId(currentUser.getId())) {
            log.warn("User {} already has a merchant profile", currentUser.getUsername());
            throw new IllegalStateException("User already has a merchant profile");
        }

        // Create new profile
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
     * Get the merchant profile for the current user.
     *
     * @return Optional containing MerchantProfileResponse if found
     */
    @Transactional(readOnly = true)
    public Optional<MerchantProfileResponse> getCurrentMerchantProfile() {
        User currentUser = getCurrentUserOrThrow();

        return merchantProfileRepository.findByUserId(currentUser.getId())
            .map(this::mapToResponse);
    }

    /**
     * Get a merchant profile by ID.
     *
     * <p>Only allows access to the current user's own profile.</p>
     *
     * @param profileId the profile ID
     * @return Optional containing MerchantProfileResponse if found and owned by current user
     */
    @Transactional(readOnly = true)
    public Optional<MerchantProfileResponse> getProfileById(Long profileId) {
        User currentUser = getCurrentUserOrThrow();

        return merchantProfileRepository.findById(profileId)
            .filter(profile -> profile.getUser().getId().equals(currentUser.getId()))
            .map(this::mapToResponse);
    }

    /**
     * Update the merchant profile for the current user.
     *
     * @param request the profile update request
     * @return MerchantProfileResponse containing the updated profile
     * @throws IllegalArgumentException if user doesn't have a merchant profile
     */
    @Transactional
    public MerchantProfileResponse updateProfile(MerchantProfileRequest request) {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        // Update fields
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
     * Get the current merchant's settings.
     *
     * @return MerchantSettingsResponse containing the settings
     * @throws IllegalArgumentException if user doesn't have a merchant profile
     */
    @Transactional(readOnly = true)
    public MerchantSettingsResponse getSettings() {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        return parseSettings(profile.getSettings());
    }

    /**
     * Update the merchant's settings.
     *
     * <p>Merges the provided settings with existing settings.</p>
     *
     * @param request the settings update request
     * @return MerchantSettingsResponse containing the updated settings
     * @throws IllegalArgumentException if user doesn't have a merchant profile
     */
    @Transactional
    public MerchantSettingsResponse updateSettings(MerchantSettingsRequest request) {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        // Parse existing settings or create new map
        @SuppressWarnings("unchecked")
        Map<String, Object> settingsMap = profile.getSettings() != null
            ? parseSettingsToMap(profile.getSettings())
            : new java.util.HashMap<>();

        // Update settings from request
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

        // Save updated settings
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
     * Delete the merchant profile for the current user.
     *
     * <p>Note: This is a hard delete. Consider implementing soft delete
     * if business requirements need to retain historical data.</p>
     *
     * @throws IllegalArgumentException if user doesn't have a merchant profile
     */
    @Transactional
    public void deleteProfile() {
        User currentUser = getCurrentUserOrThrow();

        MerchantProfile profile = merchantProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for current user"));

        merchantProfileRepository.delete(profile);
        log.info("Deleted merchant profile for user {}: id={}",
            currentUser.getUsername(), profile.getId());
    }

    /**
     * Check if the current user has a merchant profile.
     *
     * @return true if the current user has a merchant profile
     */
    @Transactional(readOnly = true)
    public boolean hasMerchantProfile() {
        User currentUser = getCurrentUserOrThrow();
        return merchantProfileRepository.existsByUserId(currentUser.getId());
    }

    /**
     * Get the current authenticated user.
     *
     * @return Optional containing the current User
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
     * Get the current authenticated user or throw an exception.
     *
     * @return the current User
     * @throws IllegalStateException if no authenticated user
     */
    private User getCurrentUserOrThrow() {
        return getCurrentUser()
            .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Map MerchantProfile entity to response DTO.
     *
     * @param profile the entity to map
     * @return the response DTO
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
     * Parse JSON settings string to MerchantSettingsResponse.
     *
     * @param settingsJson the JSON string
     * @return the settings response
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
     * Parse JSON settings string to Map.
     *
     * @param settingsJson the JSON string
     * @return the settings map
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
