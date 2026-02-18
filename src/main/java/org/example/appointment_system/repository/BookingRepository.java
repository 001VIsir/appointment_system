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
 * 预约实体Repository接口。
 *
 * <p>提供预约数据访问操作，包括：</p>
 * <ul>
 *   <li>标准CRUD操作（继承自JpaRepository）</li>
 *   <li>按用户、时段和状态查询</li>
 *   <li>按各种条件统计预约数量</li>
 *   <li>检查重复预约</li>
 * </ul>
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ============================================
    // 按用户查询
    // ============================================

    /**
     * 查询指定用户的所有预约。
     *
     * @param user 要查询的用户
     * @return 该用户的预约列表
     */
    List<Booking> findByUser(User user);

    /**
     * 按用户ID查询所有预约。
     *
     * @param userId 要查询的用户ID
     * @return 该用户的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID分页查询所有预约。
     *
     * @param userId   要查询的用户ID
     * @param pageable 分页信息
     * @return 该用户的预约分页
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 按用户和状态查询预约。
     *
     * @param user   要查询的用户
     * @param status 预约状态
     * @return 匹配条件的预约列表
     */
    List<Booking> findByUserAndStatus(User user, BookingStatus status);

    /**
     * 按用户ID和状态查询预约。
     *
     * @param userId 要查询的用户ID
     * @param status 预约状态
     * @return 匹配条件的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    List<Booking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);

    /**
     * 按用户ID和状态分页查询预约。
     *
     * @param userId   要查询的用户ID
     * @param status   预约状态
     * @param pageable 分页信息
     * @return 匹配条件的预约分页
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    Page<Booking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);

    /**
     * 按用户和状态列表查询预约。
     *
     * @param userId   要查询的用户ID
     * @param statuses 要筛选的状态列表
     * @return 匹配条件的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status IN :statuses")
    List<Booking> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<BookingStatus> statuses);

    // ============================================
    // 按时段查询
    // ============================================

    /**
     * 查询指定时段的所有预约。
     *
     * @param slot 要查询的时段
     * @return 该时段的预约列表
     */
    List<Booking> findBySlot(AppointmentSlot slot);

    /**
     *按时段ID查询所有预约。
     *
     * @param slotId 要查询的时段ID
     * @return 该时段的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId")
    List<Booking> findBySlotId(@Param("slotId") Long slotId);

    /**
     * 按时段和状态查询预约。
     *
     * @param slot   要查询的时段
     * @param status 预约状态
     * @return 匹配条件的预约列表
     */
    List<Booking> findBySlotAndStatus(AppointmentSlot slot, BookingStatus status);

    /**
     * 按时段ID和状态查询预约。
     *
     * @param slotId 要查询的时段ID
     * @param status 预约状态
     * @return 匹配条件的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId AND b.status = :status")
    List<Booking> findBySlotIdAndStatus(@Param("slotId") Long slotId, @Param("status") BookingStatus status);

    // ============================================
    // 按用户和时段查询（用于重复检查）
    // ============================================

    /**
     * 按用户和时段查询预约。
     *
     * @param user 用户
     * @param slot 时段
     * @return 包含预约的Optional（如果找到）
     */
    Optional<Booking> findByUserAndSlot(User user, AppointmentSlot slot);

    /**
     * 按用户ID和时段ID查询预约。
     *
     * @param userId 用户ID
     * @param slotId 时段ID
     * @return 包含预约的Optional（如果找到）
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.slot.id = :slotId")
    Optional<Booking> findByUserIdAndSlotId(@Param("userId") Long userId, @Param("slotId") Long slotId);

    /**
     * 检查指定用户和时段是否存在预约。
     *
     * @param userId 用户ID
     * @param slotId 时段ID
     * @return 如果存在预约返回true
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b WHERE b.user.id = :userId AND b.slot.id = :slotId")
    boolean existsByUserIdAndSlotId(@Param("userId") Long userId, @Param("slotId") Long slotId);

    /**
     * 检查指定用户和时段是否存在有效预约。
     *
     * @param userId 用户ID
     * @param slotId 时段ID
     * @return 如果存在有效预约返回true
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.user.id = :userId AND b.slot.id = :slotId AND b.status IN ('PENDING', 'CONFIRMED')")
    boolean existsActiveBookingByUserIdAndSlotId(@Param("userId") Long userId, @Param("slotId") Long slotId);

    // ============================================
    // 按状态查询
    // ============================================

    /**
     * 查询所有具有指定状态的预约。
     *
     * @param status 预约状态
     * @return 具有该状态的预约列表
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * 分页查询所有具有指定状态的预约。
     *
     * @param status   预约状态
     * @param pageable 分页信息
     * @return 具有该状态的预约分页
     */
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    /**
     * 查询状态在列表中的所有预约。
     *
     * @param statuses 要筛选的状态列表
     * @return 匹配任一状态的预约列表
     */
    List<Booking> findByStatusIn(List<BookingStatus> statuses);

    // ============================================
    // 按任务查询（通过时段）
    // ============================================

    /**
     * 按任务ID查询所有预约。
     *
     * @param taskId 要查询的任务ID
     * @return 该任务的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.id = :taskId")
    List<Booking> findByTaskId(@Param("taskId") Long taskId);

    /**
     * 按任务ID分页查询预约。
     *
     * @param taskId   要查询的任务ID
     * @param pageable 分页信息
     * @return 该任务的预约分页
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.id = :taskId")
    Page<Booking> findByTaskId(@Param("taskId") Long taskId, Pageable pageable);

    /**
     * 按任务ID和状态查询预约。
     *
     * @param taskId 要查询的任务ID
     * @param status 预约状态
     * @return 匹配条件的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.id = :taskId AND b.status = :status")
    List<Booking> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") BookingStatus status);

    // ============================================
    // 按商家查询（通过时段 -> 任务 -> 服务 -> 商家）
    // ============================================

    /**
     * 按商家ID查询所有预约。
     *
     * @param merchantId 要查询的商家ID
     * @return 该商家的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId")
    List<Booking> findByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 按商家ID分页查询预约。
     *
     * @param merchantId 要查询的商家ID
     * @param pageable   分页信息
     * @return 该商家的预约分页
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId")
    Page<Booking> findByMerchantId(@Param("merchantId") Long merchantId, Pageable pageable);

    /**
     * 按商家ID和状态查询预约。
     *
     * @param merchantId 要查询的商家ID
     * @param status     预约状态
     * @return 匹配条件的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId AND b.status = :status")
    List<Booking> findByMerchantIdAndStatus(@Param("merchantId") Long merchantId, @Param("status") BookingStatus status);

    /**
     * 按商家ID和状态分页查询预约。
     *
     * @param merchantId 要查询的商家ID
     * @param status     预约状态
     * @param pageable   分页信息
     * @return 匹配条件的预约分页
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId AND b.status = :status")
    Page<Booking> findByMerchantIdAndStatus(@Param("merchantId") Long merchantId, @Param("status") BookingStatus status, Pageable pageable);

    // ============================================
    // 计数操作
    // ============================================

    /**
     * 统计指定用户的所有预约数量。
     *
     * @param userId 用户ID
     * @return 预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID和状态统计预约数量。
     *
     * @param userId 用户ID
     * @param status 预约状态
     * @return 预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);

    /**
     * 统计指定时段的所有预约数量。
     *
     * @param slotId 时段ID
     * @return 预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.id = :slotId")
    long countBySlotId(@Param("slotId") Long slotId);

    /**
     * 统计指定时段的有效预约数量（非取消、非完成）。
     *
     * @param slotId 时段ID
     * @return 有效预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.id = :slotId AND b.status IN ('PENDING', 'CONFIRMED')")
    long countActiveBySlotId(@Param("slotId") Long slotId);

    /**
     * 统计指定任务的预约数量。
     *
     * @param taskId 任务ID
     * @return 预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.task.id = :taskId")
    long countByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计指定任务的有效预约数量。
     *
     * @param taskId 任务ID
     * @return 有效预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.task.id = :taskId AND b.status IN ('PENDING', 'CONFIRMED')")
    long countActiveByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计指定商家的预约数量。
     *
     * @param merchantId 商家ID
     * @return 预约数量
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId")
    long countByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 统计所有具有指定状态的预约数量。
     *
     * @param status 预约状态
     * @return 预约数量
     */
    long countByStatus(BookingStatus status);

    // ============================================
    // 按日期范围查询
    // ============================================

    /**
     * 查询在指定日期范围内创建的预约。
     *
     * @param start 开始日期时间（包含）
     * @param end   结束日期时间（不包含）
     * @return 日期范围内的预约列表
     */
    List<Booking> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查询指定用户在指定日期范围内创建的预约。
     *
     * @param userId 用户ID
     * @param start  开始日期时间（包含）
     * @param end    结束日期时间（不包含）
     * @return 日期范围内的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.createdAt >= :start AND b.createdAt < :end")
    List<Booking> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 查询指定商家在指定日期范围内创建的预约。
     *
     * @param merchantId 商家ID
     * @param start      开始日期时间（包含）
     * @param end        结束日期时间（不包含）
     * @return 日期范围内的预约列表
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.task.service.merchant.id = :merchantId " +
           "AND b.createdAt >= :start AND b.createdAt < :end")
    List<Booking> findByMerchantIdAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ============================================
    // 按ID查询（含检查）
    // ============================================

    /**
     * 按ID和用户ID查询预约。
     *
     * @param id     预约ID
     * @param userId 用户ID
     * @return 包含预约的Optional（如果找到且属于该用户）
     */
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.user.id = :userId")
    Optional<Booking> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 按ID和时段ID查询预约。
     *
     * @param id     预约ID
     * @param slotId 时段ID
     * @return 包含预约的Optional（如果找到且属于该时段）
     */
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.slot.id = :slotId")
    Optional<Booking> findByIdAndSlotId(@Param("id") Long id, @Param("slotId") Long slotId);

    // ============================================
    // 删除操作
    // ============================================

    /**
     * 删除指定用户的所有预约。
     *
     * @param userId 用户ID
     */
    @Modifying
    @Query(value = "DELETE FROM bookings WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除指定时段的所有预约。
     *
     * @param slotId 时段ID
     */
    @Modifying
    @Query(value = "DELETE FROM bookings WHERE slot_id = :slotId", nativeQuery = true)
    void deleteBySlotId(@Param("slotId") Long slotId);
}
