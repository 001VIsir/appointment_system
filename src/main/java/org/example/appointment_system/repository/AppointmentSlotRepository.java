package org.example.appointment_system.repository;

import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 预约时段实体Repository接口。
 *
 * <p>提供预约时段数据访问操作，包括：</p>
 * <ul>
 *   <li>标准CRUD操作（继承自JpaRepository）</li>
 *   <li>按预约任务查询</li>
 *   <li>查询可用时段（有剩余容量的）</li>
 *   <li>检查时段可用性</li>
 * </ul>
 */
@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    /**
     * 查询指定预约任务的所有时段。
     *
     * @param task 要查询的预约任务
     * @return 该任务的时段列表
     */
    List<AppointmentSlot> findByTask(AppointmentTask task);

    /**
     * 按预约任务ID查询所有时段。
     *
     * @param taskId 要查询的预约任务ID
     * @return 该任务的时段列表
     */
    List<AppointmentSlot> findByTaskId(Long taskId);

    /**
     * 按ID和任务查询时段。
     *
     * @param id   时段ID
     * @param task 预约任务
     * @return 包含时段的Optional（如果找到且属于该任务）
     */
    Optional<AppointmentSlot> findByIdAndTask(Long id, AppointmentTask task);

    /**
     * 按ID和任务ID查询时段。
     *
     * @param id     时段ID
     * @param taskId 预约任务ID
     * @return 包含时段的Optional（如果找到）
     */
    Optional<AppointmentSlot> findByIdAndTaskId(Long id, Long taskId);

    /**
     * 查询指定任务的有可用容量的所有时段。
     * 条件为 booked_count < capacity。
     *
     * @param task 预约任务
     * @return 有可用容量的时段列表
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task = :task AND s.bookedCount < s.capacity")
    List<AppointmentSlot> findAvailableSlotsByTask(@Param("task") AppointmentTask task);

    /**
     * 按任务ID查询有可用容量的所有时段。
     *
     * @param taskId 预约任务ID
     * @return 有可用容量的时段列表
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task.id = :taskId AND s.bookedCount < s.capacity")
    List<AppointmentSlot> findAvailableSlotsByTaskId(@Param("taskId") Long taskId);

    /**
     * 按任务查询时段并按开始时间排序。
     *
     * @param task 预约任务
     * @return 按开始时间升序的时段列表
     */
    List<AppointmentSlot> findByTaskOrderByStartTimeAsc(AppointmentTask task);

    /**
     * 按任务ID查询时段并按开始时间排序。
     *
     * @param taskId 预约任务ID
     * @return 按开始时间升序的时段列表
     */
    List<AppointmentSlot> findByTaskIdOrderByStartTimeAsc(Long taskId);

    /**
     * 按任务和开始时间查询时段。
     *
     * @param task      预约任务
     * @param startTime 开始时间
     * @return 包含时段的Optional（如果找到）
     */
    Optional<AppointmentSlot> findByTaskAndStartTime(AppointmentTask task, LocalTime startTime);

    /**
     * 按任务ID和开始时间查询时段。
     *
     * @param taskId    预约任务ID
     * @param startTime 开始时间
     * @return 包含时段的Optional（如果找到）
     */
    Optional<AppointmentSlot> findByTaskIdAndStartTime(Long taskId, LocalTime startTime);

    /**
     * 按任务和时间范围查询时段。
     *
     * @param task      预约任务
     * @param startTime 开始时间（包含）
     * @param endTime   结束时间（包含）
     * @return 时间范围内的时段列表
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task = :task " +
           "AND s.startTime >= :startTime AND s.endTime <= :endTime")
    List<AppointmentSlot> findByTaskAndTimeRange(
            @Param("task") AppointmentTask task,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * 统计指定任务的所有时段数量。
     *
     * @param task 预约任务
     * @return 时段数量
     */
    long countByTask(AppointmentTask task);

    /**
     * 统计指定任务的有可用容量的时段数量。
     *
     * @param task 预约任务
     * @return 可用时段数量
     */
    @Query("SELECT COUNT(s) FROM AppointmentSlot s WHERE s.task = :task AND s.bookedCount < s.capacity")
    long countAvailableSlotsByTask(@Param("task") AppointmentTask task);

    /**
     * 统计指定任务的总容量（所有时段容量之和）。
     *
     * @param taskId 预约任务ID
     * @return 总容量
     */
    @Query("SELECT COALESCE(SUM(s.capacity), 0) FROM AppointmentSlot s WHERE s.task.id = :taskId")
    int sumCapacityByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计指定任务的总预约数（所有时段预约数之和）。
     *
     * @param taskId 预约任务ID
     * @return 总预约数
     */
    @Query("SELECT COALESCE(SUM(s.bookedCount), 0) FROM AppointmentSlot s WHERE s.task.id = :taskId")
    int sumBookedCountByTaskId(@Param("taskId") Long taskId);

    /**
     * 检查指定任务是否存在指定开始时间的时段。
     *
     * @param task      预约任务
     * @param startTime 要检查的开始时间
     * @return 如果存在此开始时间的时段返回true
     */
    boolean existsByTaskAndStartTime(AppointmentTask task, LocalTime startTime);

    /**
     * 检查指定任务是否存在指定时间范围的时段。
     *
     * @param task      预约任务
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 如果存在此时间范围的时段返回true
     */
    boolean existsByTaskAndStartTimeAndEndTime(AppointmentTask task, LocalTime startTime, LocalTime endTime);

    /**
     * 删除指定任务的所有时段。
     *
     * @param task 预约任务
     */
    void deleteByTask(AppointmentTask task);

    /**
     * 按任务ID删除所有时段。
     *
     * @param taskId 预约任务ID
     */
    void deleteByTaskId(Long taskId);

    /**
     * 查询指定任务的已满时段。
     *
     * @param task 预约任务
     * @return 已满时段列表
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task = :task AND s.bookedCount >= s.capacity")
    List<AppointmentSlot> findFullyBookedSlotsByTask(@Param("task") AppointmentTask task);
}
