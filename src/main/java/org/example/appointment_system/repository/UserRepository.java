package org.example.appointment_system.repository;

import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 *
 * <p>Provides data access operations for users including:</p>
 * <ul>
 *   <li>Standard CRUD operations (inherited from JpaRepository)</li>
 *   <li>Find by username (for authentication)</li>
 *   <li>Find by email (for uniqueness checks)</li>
 *   <li>Find by role (for user management)</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * <p>Used during authentication to load user details.</p>
     *
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email address.
     *
     * <p>Used for email uniqueness validation during registration.</p>
     *
     * @param email the email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a username already exists.
     *
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email already exists.
     *
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all users by role.
     *
     * <p>Used by administrators to list users by type.</p>
     *
     * @param role the role to filter by
     * @return list of users with the specified role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all enabled users.
     *
     * @return list of enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find all disabled users.
     *
     * @return list of disabled users
     */
    List<User> findByEnabledFalse();

    /**
     * Find users by role and enabled status.
     *
     * @param role    the role to filter by
     * @param enabled the enabled status
     * @return list of matching users
     */
    List<User> findByRoleAndEnabled(UserRole role, boolean enabled);

    /**
     * Count users by role.
     *
     * @param role the role to count
     * @return number of users with the specified role
     */
    long countByRole(UserRole role);
}
