package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for service item management operations.
 *
 * <p>Handles CRUD operations for service items including:</p>
 * <ul>
 *   <li>Creating service items for merchants</li>
 *   <li>Updating service item information</li>
 *   <li>Soft-deleting service items (setting active=false)</li>
 *   <li>Retrieving service items for merchants</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>All operations require the current user to have MERCHANT role and an existing
 * merchant profile. Users can only access and modify their own service items.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final UserRepository userRepository;

    /**
     * Create a new service item for the current merchant.
     *
     * <p>The current user must have a merchant profile. Service names must be
     * unique within a merchant's catalog.</p>
     *
     * @param request the service item creation request
     * @return ServiceItemResponse containing the created service item
     * @throws IllegalStateException if user doesn't have a merchant profile
     * @throws IllegalArgumentException if a service with the same name already exists
     */
    @Transactional
    public ServiceItemResponse createServiceItem(ServiceItemRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        // Check for duplicate service name
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
     * Update an existing service item.
     *
     * <p>Only the merchant who owns the service item can update it.
     * If the name is being changed, the new name must not conflict with
     * existing service names.</p>
     *
     * @param serviceId the ID of the service item to update
     * @param request the update request
     * @return ServiceItemResponse containing the updated service item
     * @throws IllegalArgumentException if service not found or not owned by current merchant
     */
    @Transactional
    public ServiceItemResponse updateServiceItem(Long serviceId, ServiceItemRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        ServiceItem serviceItem = serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service item not found or not owned by current merchant"));

        // Check for duplicate name if name is being changed
        if (!serviceItem.getName().equals(request.getName()) &&
            serviceItemRepository.existsByMerchantAndName(merchantProfile, request.getName())) {
            log.warn("Cannot rename service to '{}' - name already exists for merchant {}",
                request.getName(), merchantProfile.getId());
            throw new IllegalArgumentException("A service item with this name already exists");
        }

        // Update fields
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
     * Soft delete a service item by setting active=false.
     *
     * <p>Only the merchant who owns the service item can delete it.
     * This performs a soft delete - the record remains in the database
     * but is hidden from active listings.</p>
     *
     * @param serviceId the ID of the service item to delete
     * @throws IllegalArgumentException if service not found or not owned by current merchant
     */
    @Transactional
    public void deleteServiceItem(Long serviceId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        ServiceItem serviceItem = serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service item not found or not owned by current merchant"));

        // Soft delete by setting active=false
        serviceItem.setActive(false);
        serviceItemRepository.save(serviceItem);
        log.info("Soft deleted service item {} for merchant {}", serviceId, merchantProfile.getId());
    }

    /**
     * Reactivate a previously soft-deleted service item.
     *
     * @param serviceId the ID of the service item to reactivate
     * @return ServiceItemResponse containing the reactivated service item
     * @throws IllegalArgumentException if service not found or not owned by current merchant
     */
    @Transactional
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
     * Get a service item by ID.
     *
     * <p>Only returns service items owned by the current merchant.</p>
     *
     * @param serviceId the ID of the service item
     * @return Optional containing ServiceItemResponse if found and owned by current merchant
     */
    @Transactional(readOnly = true)
    public Optional<ServiceItemResponse> getServiceItemById(Long serviceId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .map(this::mapToResponse);
    }

    /**
     * Get all service items for the current merchant.
     *
     * <p>Returns both active and inactive service items.</p>
     *
     * @return list of ServiceItemResponse for all merchant's services
     */
    @Transactional(readOnly = true)
    public List<ServiceItemResponse> getAllServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchant(merchantProfile).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get all active service items for the current merchant.
     *
     * <p>Only returns service items that are available for booking.</p>
     *
     * @return list of ServiceItemResponse for active services
     */
    @Transactional(readOnly = true)
    public List<ServiceItemResponse> getActiveServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchantAndActiveTrue(merchantProfile).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get all inactive (soft-deleted) service items for the current merchant.
     *
     * <p>Useful for managing/reactivate previously deleted services.</p>
     *
     * @return list of ServiceItemResponse for inactive services
     */
    @Transactional(readOnly = true)
    public List<ServiceItemResponse> getInactiveServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchantAndActiveFalse(merchantProfile).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Count total service items for the current merchant.
     *
     * @return count of all service items
     */
    @Transactional(readOnly = true)
    public long countAllServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();
        return serviceItemRepository.countByMerchant(merchantProfile);
    }

    /**
     * Count active service items for the current merchant.
     *
     * @return count of active service items
     */
    @Transactional(readOnly = true)
    public long countActiveServiceItems() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();
        return serviceItemRepository.countByMerchantAndActiveTrue(merchantProfile);
    }

    /**
     * Check if a service item with the given name exists for the current merchant.
     *
     * @param name the service name to check
     * @return true if a service with this name exists
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();
        return serviceItemRepository.existsByMerchantAndName(merchantProfile, name);
    }

    /**
     * Get the current authenticated user's merchant profile.
     *
     * @return Optional containing the MerchantProfile
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
     * Get the current authenticated user's merchant profile or throw an exception.
     *
     * @return the MerchantProfile
     * @throws IllegalStateException if no authenticated user or no merchant profile
     */
    private MerchantProfile getCurrentMerchantProfileOrThrow() {
        return getCurrentMerchantProfile()
            .orElseThrow(() -> new IllegalStateException(
                "No merchant profile found for current user. Please create a merchant profile first."));
    }

    /**
     * Map ServiceItem entity to response DTO.
     *
     * @param serviceItem the entity to map
     * @return the response DTO
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
