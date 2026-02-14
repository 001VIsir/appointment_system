package org.example.appointment_system.repository;

import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for MerchantProfile entity.
 *
 * <p>Provides data access operations for merchant profiles including:</p>
 * <ul>
 *   <li>Standard CRUD operations (inherited from JpaRepository)</li>
 *   <li>Find by user ID (for looking up merchant by user)</li>
 *   <li>Find by user entity</li>
 *   <li>Check existence by user ID</li>
 * </ul>
 */
@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {

    /**
     * Find a merchant profile by the associated user's ID.
     *
     * <p>Used to look up a merchant's profile given their user account ID.</p>
     *
     * @param userId the user ID to search for
     * @return Optional containing the merchant profile if found
     */
    Optional<MerchantProfile> findByUserId(Long userId);

    /**
     * Find a merchant profile by the associated user entity.
     *
     * <p>Alternative method using the User entity directly.</p>
     *
     * @param user the user entity to search for
     * @return Optional containing the merchant profile if found
     */
    Optional<MerchantProfile> findByUser(User user);

    /**
     * Check if a merchant profile exists for the given user ID.
     *
     * <p>Used to validate that a user already has a merchant profile
     * before creating a new one.</p>
     *
     * @param userId the user ID to check
     * @return true if a profile exists for this user, false otherwise
     */
    boolean existsByUserId(Long userId);

    /**
     * Check if a merchant profile exists for the given user entity.
     *
     * @param user the user entity to check
     * @return true if a profile exists for this user, false otherwise
     */
    boolean existsByUser(User user);

    /**
     * Find a merchant profile by business name.
     *
     * <p>Used for searching merchants by their business name.
     * Note: Business names are not unique, so this returns Optional.</p>
     *
     * @param businessName the business name to search for
     * @return Optional containing the first matching merchant profile
     */
    Optional<MerchantProfile> findByBusinessName(String businessName);

    /**
     * Check if a merchant profile exists with the given business name.
     *
     * @param businessName the business name to check
     * @return true if any profile has this business name
     */
    boolean existsByBusinessName(String businessName);
}
