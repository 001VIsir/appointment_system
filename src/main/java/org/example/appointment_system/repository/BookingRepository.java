package org.example.appointment_system.repository;

import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Booking entity.
 *
 * <p>Provides data access operations for bookings including:</p>
 * <ul>
 *   <li>Standard CRUD operations (inherited from JpaRepository)</li>
 *   <li>Find by user, slot, and status</li>
 *   <li>Count bookings by various criteria</li>
 *   <li>Check for duplicate bookings</li>
 * </ul>
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ============================================
    // Find by User
    // ============================================

    /**
     * Find all bookings for a specific user.
     *
     * @param user the user to search by
     * @return list of bookings for the user
     */
    List<Booking> findByUser(User user);

    /**
     * Find all bookings for a specific user by user ID.
     *
     * @param userId the user ID to search by
     * @return list of bookings for the user
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(@Param("userId") Long userId);

    /**
     * Find all bookings for a specific user with pagination.
     *
     * @param userId   the user ID to search by
     * @param pageable pagination information
     * @return page of bookings for the user
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find bookings by user and status.
     *
     * @param user   the user to search by
     * @param status the booking status
     * @return list of bookings matching the criteria
     */
    List<Booking> findByUserAndStatus(User user, BookingStatus status);

    /**
     * Find bookings by user ID and status.
     *
     * @param userId the user ID to search by
     * @param status the booking status
     * @return list of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    List<Booking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);

    /**
     * Find bookings by user ID and status with pagination.
     *
     * @param userId   the user ID to search by
     * @param status   the booking status
     * @param pageable pagination information
     * @return page of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    Page<Booking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);

    /**
     * Find bookings by user with status in a list.
     *
     * @param userId  the user ID to search by
     * @param statuses list of statuses to filter by
     * @return list of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status IN :statuses")
    List<Booking> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<BookingStatus> statuses);

    // ============================================
    // Find by Slot
    // ============================================

    /**
     * Find all bookings for a specific slot.
     *
     * @param slot the slot to search by
     * @return list of bookings for the slot
     */
    List<Booking> findBySlot(AppointmentSlot slot);

    /**
     * Find all bookings for a specific slot by slot ID.
     *
     * @param slotId the slot ID to search by
     * @return list of bookings for the slot
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId")
    List<Booking> findBySlotId(@Param("slotId") Long slotId);

    /**
     * Find bookings by slot and status.
     *
     * @param slot   the slot to search by
     * @param status the booking status
     * @return list of bookings matching the criteria
     */
    List<Booking> findBySlotAndStatus(AppointmentSlot slot, BookingStatus status);

    /**
     * Find bookings by slot ID and status.
     *
     * @param slotId the slot ID to search by
     * @param status the booking status
     * @return list of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId AND b.status = :status")
    List<Booking> findBySlotIdAndStatus(@Param("slotId") Long slotId, @Param("status") BookingStatus status);

    // ============================================
    // Find by User and Slot (for duplicate check)
    // ============================================

    /**
     * Find a booking by user and slot.
     *
     * @param user the user
     * @param slot the slot
     * @return Optional containing the booking if found
     */
    Optional<Booking> findByUserAndSlot(User user, AppointmentSlot slot);

    /**
     * Find a booking by user ID and slot ID.
     *
     * @param userId the user ID
     * @param slotId the slot ID
     * @return Optional containing the booking if found
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.slot.id = :slotId")
    Optional<Booking> findByUserIdAndSlotId(@Param("userId") Long userId, @Param("slotId") Long slotId);

    /**
     * Check if a booking exists for a user and slot.
     *
     * @param userId the user ID
     * @param slotId the slot ID
     * @return true if a booking exists
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b WHERE b.user.id = :userId AND b.slot.id = :slotId")
    boolean existsByUserIdAndSlotId(@Param("userId") Long userId, @Param("slotId") Long slotId);

    /**
     * Check if an active booking exists for a user and slot.
     *
     * @param userId the user ID
     * @param slotId the slot ID
     * @return true if an active booking exists
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.user.id = :userId AND b.slot.id = :slotId AND b.status IN ('PENDING', 'CONFIRMED')")
    boolean existsActiveBookingByUserIdAndSlotId(@Param("userId") Long userId, @Param("slotId") Long slotId);

    // ============================================
    // Find by Status
    // ============================================

    /**
     * Find all bookings with a specific status.
     *
     * @param status the booking status
     * @return list of bookings with the status
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Find all bookings with a specific status with pagination.
     *
     * @param status   the booking status
     * @param pageable pagination information
     * @return page of bookings with the status
     */
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    /**
     * Find all bookings with status in a list.
     *
     * @param statuses list of statuses to filter by
     * @return list of bookings matching any of the statuses
     */
    List<Booking> findByStatusIn(List<BookingStatus> statuses);

    // ============================================
    // Find by Task (through slot)
    // ============================================

    /**
     * Find all bookings for a specific task by task ID.
     *
     * @param taskId the task ID to search by
     * @return list of bookings for the task
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.id = :taskId")
    List<Booking> findByTaskId(@Param("taskId") Long taskId);

    /**
     * Find bookings for a task by task ID with pagination.
     *
     * @param taskId   the task ID to search by
     * @param pageable pagination information
     * @return page of bookings for the task
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.id = :taskId")
    Page<Booking> findByTaskId(@Param("taskId") Long taskId, Pageable pageable);

    /**
     * Find bookings for a task by task ID and status.
     *
     * @param taskId the task ID to search by
     * @param status the booking status
     * @return list of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.id = :taskId AND b.status = :status")
    List<Booking> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") BookingStatus status);

    // ============================================
    // Find by Merchant (through slot -> task -> service -> merchant)
    // ============================================

    /**
     * Find all bookings for a specific merchant by merchant ID.
     *
     * @param merchantId the merchant ID to search by
     * @return list of bookings for the merchant
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId")
    List<Booking> findByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * Find bookings for a merchant by merchant ID with pagination.
     *
     * @param merchantId the merchant ID to search by
     * @param pageable   pagination information
     * @return page of bookings for the merchant
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId")
    Page<Booking> findByMerchantId(@Param("merchantId") Long merchantId, Pageable pageable);

    /**
     * Find bookings for a merchant by merchant ID and status.
     *
     * @param merchantId the merchant ID to search by
     * @param status     the booking status
     * @return list of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId AND b.status = :status")
    List<Booking> findByMerchantIdAndStatus(@Param("merchantId") Long merchantId, @Param("status") BookingStatus status);

    /**
     * Find bookings for a merchant by merchant ID and status with pagination.
     *
     * @param merchantId the merchant ID to search by
     * @param status     the booking status
     * @param pageable   pagination information
     * @return page of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId AND b.status = :status")
    Page<Booking> findByMerchantIdAndStatus(@Param("merchantId") Long merchantId, @Param("status") BookingStatus status, Pageable pageable);

    // ============================================
    // Count Operations
    // ============================================

    /**
     * Count all bookings for a user.
     *
     * @param userId the user ID
     * @return the count of bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Count bookings for a user by status.
     *
     * @param userId the user ID
     * @param status the booking status
     * @return the count of bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);

    /**
     * Count all bookings for a slot.
     *
     * @param slotId the slot ID
     * @return the count of bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.id = :slotId")
    long countBySlotId(@Param("slotId") Long slotId);

    /**
     * Count active (non-cancelled, non-completed) bookings for a slot.
     *
     * @param slotId the slot ID
     * @return the count of active bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.id = :slotId AND b.status IN ('PENDING', 'CONFIRMED')")
    long countActiveBySlotId(@Param("slotId") Long slotId);

    /**
     * Count bookings for a task.
     *
     * @param taskId the task ID
     * @return the count of bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.task.id = :taskId")
    long countByTaskId(@Param("taskId") Long taskId);

    /**
     * Count active bookings for a task.
     *
     * @param taskId the task ID
     * @return the count of active bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.task.id = :taskId AND b.status IN ('PENDING', 'CONFIRMED')")
    long countActiveByTaskId(@Param("taskId") Long taskId);

    /**
     * Count bookings for a merchant.
     *
     * @param merchantId the merchant ID
     * @return the count of bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId")
    long countByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * Count all bookings with a specific status.
     *
     * @param status the booking status
     * @return the count of bookings
     */
    long countByStatus(BookingStatus status);

    // ============================================
    // Find by Date Range
    // ============================================

    /**
     * Find bookings created within a date range.
     *
     * @param start the start datetime (inclusive)
     * @param end   the end datetime (exclusive)
     * @return list of bookings in the date range
     */
    List<Booking> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find bookings for a user created within a date range.
     *
     * @param userId the user ID
     * @param start  the start datetime (inclusive)
     * @param end    the end datetime (exclusive)
     * @return list of bookings in the date range
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.createdAt >= :start AND b.createdAt < :end")
    List<Booking> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find bookings for a merchant created within a date range.
     *
     * @param merchantId the merchant ID
     * @param start      the start datetime (inclusive)
     * @param end        the end datetime (exclusive)
     * @return list of bookings in the date range
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId " +
           "AND b.createdAt >= :start AND b.createdAt < :end")
    List<Booking> findByMerchantIdAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ============================================
    // Find by ID with checks
    // ============================================

    /**
     * Find a booking by ID and user ID.
     *
     * @param id     the booking ID
     * @param userId the user ID
     * @return Optional containing the booking if found and belongs to the user
     */
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.user.id = :userId")
    Optional<Booking> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Find a booking by ID and slot ID.
     *
     * @param id     the booking ID
     * @param slotId the slot ID
     * @return Optional containing the booking if found and belongs to the slot
     */
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.slot.id = :slotId")
    Optional<Booking> findByIdAndSlotId(@Param("id") Long id, @Param("slotId") Long slotId);

    // ============================================
    // Delete Operations
    // ============================================

    /**
     * Delete all bookings for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query(value = "DELETE FROM bookings WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Delete all bookings for a slot.
     *
     * @param slotId the slot ID
     */
    @Modifying
    @Query(value = "DELETE FROM bookings WHERE slot_id = :slotId", nativeQuery = true)
    void deleteBySlotId(@Param("slotId") Long slotId);
}
