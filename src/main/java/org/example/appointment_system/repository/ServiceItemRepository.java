package org.example.appointment_system.repository;

import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ServiceItem entity.
 *
 * <p>Provides data access operations for service items including:</p>
 * <ul>
 *   <li>Standard CRUD operations (inherited from JpaRepository)</li>
 *   <li>Find by merchant profile</li>
 *   <li>Find by category and active status</li>
 *   <li>Check existence by merchant and name</li>
 * </ul>
 */
@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    /**
     * Find all service items belonging to a specific merchant.
     *
     * <p>Returns both active and inactive services for the merchant.</p>
     *
     * @param merchant the merchant profile to search by
     * @return list of service items for the merchant
     */
    List<ServiceItem> findByMerchant(MerchantProfile merchant);

    /**
     * Find all service items belonging to a specific merchant by merchant ID.
     *
     * <p>Convenience method using merchant ID directly.</p>
     *
     * @param merchantId the merchant profile ID to search by
     * @return list of service items for the merchant
     */
    List<ServiceItem> findByMerchantId(Long merchantId);

    /**
     * Find all active service items belonging to a specific merchant.
     *
     * <p>Used to display available services to customers.</p>
     *
     * @param merchant the merchant profile to search by
     * @return list of active service items for the merchant
     */
    List<ServiceItem> findByMerchantAndActiveTrue(MerchantProfile merchant);

    /**
     * Find all active service items belonging to a specific merchant by merchant ID.
     *
     * @param merchantId the merchant profile ID to search by
     * @return list of active service items for the merchant
     */
    List<ServiceItem> findByMerchantIdAndActiveTrue(Long merchantId);

    /**
     * Find a service item by ID and merchant.
     *
     * <p>Used to verify that a service belongs to a specific merchant
     * before performing operations on it.</p>
     *
     * @param id       the service item ID
     * @param merchant the merchant profile
     * @return Optional containing the service item if found and belongs to the merchant
     */
    Optional<ServiceItem> findByIdAndMerchant(Long id, MerchantProfile merchant);

    /**
     * Find a service item by ID and merchant ID.
     *
     * @param id         the service item ID
     * @param merchantId the merchant profile ID
     * @return Optional containing the service item if found
     */
    Optional<ServiceItem> findByIdAndMerchantId(Long id, Long merchantId);

    /**
     * Find all service items by category.
     *
     * <p>Used for filtering services by category across all merchants.</p>
     *
     * @param category the service category to filter by
     * @return list of service items in the specified category
     */
    List<ServiceItem> findByCategory(ServiceCategory category);

    /**
     * Find all active service items by category.
     *
     * @param category the service category to filter by
     * @return list of active service items in the specified category
     */
    List<ServiceItem> findByCategoryAndActiveTrue(ServiceCategory category);

    /**
     * Find all active service items for a merchant by category.
     *
     * @param merchant the merchant profile
     * @param category the service category
     * @return list of active service items matching the criteria
     */
    List<ServiceItem> findByMerchantAndCategoryAndActiveTrue(MerchantProfile merchant, ServiceCategory category);

    /**
     * Check if a service item with the given name exists for a merchant.
     *
     * <p>Used to prevent duplicate service names within a merchant's catalog.</p>
     *
     * @param merchant the merchant profile
     * @param name     the service name to check
     * @return true if a service with this name exists for the merchant
     */
    boolean existsByMerchantAndName(MerchantProfile merchant, String name);

    /**
     * Check if a service item with the given name exists for a merchant by merchant ID.
     *
     * @param merchantId the merchant profile ID
     * @param name       the service name to check
     * @return true if a service with this name exists for the merchant
     */
    boolean existsByMerchantIdAndName(Long merchantId, String name);

    /**
     * Count all service items for a merchant.
     *
     * @param merchant the merchant profile
     * @return the count of service items
     */
    long countByMerchant(MerchantProfile merchant);

    /**
     * Count active service items for a merchant.
     *
     * @param merchant the merchant profile
     * @return the count of active service items
     */
    long countByMerchantAndActiveTrue(MerchantProfile merchant);

    /**
     * Find all inactive service items for a merchant.
     *
     * <p>Used for merchant to manage/reactivate disabled services.</p>
     *
     * @param merchant the merchant profile
     * @return list of inactive service items
     */
    List<ServiceItem> findByMerchantAndActiveFalse(MerchantProfile merchant);
}
