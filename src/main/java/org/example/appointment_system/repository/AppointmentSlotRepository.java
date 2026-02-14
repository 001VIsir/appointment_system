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
 * Repository interface for AppointmentSlot entity.
 *
 * <p>Provides data access operations for appointment slots including:</p>
 * <ul>
 *   <li>Standard CRUD operations (inherited from JpaRepository)</li>
 *   <li>Find by appointment task</li>
 *   <li>Find available slots (with capacity)</li>
 *   <li>Check slot availability</li>
 * </ul>
 */
@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    /**
     * Find all slots belonging to a specific appointment task.
     *
     * @param task the appointment task to search by
     * @return list of slots for the task
     */
    List<AppointmentSlot> findByTask(AppointmentTask task);

    /**
     * Find all slots belonging to a specific appointment task by task ID.
     *
     * @param taskId the appointment task ID to search by
     * @return list of slots for the task
     */
    List<AppointmentSlot> findByTaskId(Long taskId);

    /**
     * Find a slot by ID and task.
     *
     * @param id   the slot ID
     * @param task the appointment task
     * @return Optional containing the slot if found and belongs to the task
     */
    Optional<AppointmentSlot> findByIdAndTask(Long id, AppointmentTask task);

    /**
     * Find a slot by ID and task ID.
     *
     * @param id     the slot ID
     * @param taskId the appointment task ID
     * @return Optional containing the slot if found
     */
    Optional<AppointmentSlot> findByIdAndTaskId(Long id, Long taskId);

    /**
     * Find all slots with available capacity for a task.
     * Slots where booked_count < capacity.
     *
     * @param task the appointment task
     * @return list of slots with available capacity
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task = :task AND s.bookedCount < s.capacity")
    List<AppointmentSlot> findAvailableSlotsByTask(@Param("task") AppointmentTask task);

    /**
     * Find all slots with available capacity for a task by task ID.
     *
     * @param taskId the appointment task ID
     * @return list of slots with available capacity
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task.id = :taskId AND s.bookedCount < s.capacity")
    List<AppointmentSlot> findAvailableSlotsByTaskId(@Param("taskId") Long taskId);

    /**
     * Find slots by task ordered by start time.
     *
     * @param task the appointment task
     * @return list of slots ordered by start time ascending
     */
    List<AppointmentSlot> findByTaskOrderByStartTimeAsc(AppointmentTask task);

    /**
     * Find slots by task ID ordered by start time.
     *
     * @param taskId the appointment task ID
     * @return list of slots ordered by start time ascending
     */
    List<AppointmentSlot> findByTaskIdOrderByStartTimeAsc(Long taskId);

    /**
     * Find a slot by task and start time.
     *
     * @param task      the appointment task
     * @param startTime the start time
     * @return Optional containing the slot if found
     */
    Optional<AppointmentSlot> findByTaskAndStartTime(AppointmentTask task, LocalTime startTime);

    /**
     * Find a slot by task ID and start time.
     *
     * @param taskId    the appointment task ID
     * @param startTime the start time
     * @return Optional containing the slot if found
     */
    Optional<AppointmentSlot> findByTaskIdAndStartTime(Long taskId, LocalTime startTime);

    /**
     * Find slots by task and time range.
     *
     * @param task      the appointment task
     * @param startTime the start time (inclusive)
     * @param endTime   the end time (inclusive)
     * @return list of slots in the time range
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task = :task " +
           "AND s.startTime >= :startTime AND s.endTime <= :endTime")
    List<AppointmentSlot> findByTaskAndTimeRange(
            @Param("task") AppointmentTask task,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Count all slots for a task.
     *
     * @param task the appointment task
     * @return the count of slots
     */
    long countByTask(AppointmentTask task);

    /**
     * Count slots with available capacity for a task.
     *
     * @param task the appointment task
     * @return the count of available slots
     */
    @Query("SELECT COUNT(s) FROM AppointmentSlot s WHERE s.task = :task AND s.bookedCount < s.capacity")
    long countAvailableSlotsByTask(@Param("task") AppointmentTask task);

    /**
     * Count total capacity for a task (sum of all slot capacities).
     *
     * @param taskId the appointment task ID
     * @return the total capacity
     */
    @Query("SELECT COALESCE(SUM(s.capacity), 0) FROM AppointmentSlot s WHERE s.task.id = :taskId")
    int sumCapacityByTaskId(@Param("taskId") Long taskId);

    /**
     * Count total booked count for a task (sum of all slot booked counts).
     *
     * @param taskId the appointment task ID
     * @return the total booked count
     */
    @Query("SELECT COALESCE(SUM(s.bookedCount), 0) FROM AppointmentSlot s WHERE s.task.id = :taskId")
    int sumBookedCountByTaskId(@Param("taskId") Long taskId);

    /**
     * Check if a slot exists with the given start time for a task.
     *
     * @param task      the appointment task
     * @param startTime the start time to check
     * @return true if a slot with this start time exists
     */
    boolean existsByTaskAndStartTime(AppointmentTask task, LocalTime startTime);

    /**
     * Check if a slot exists with the given time range for a task.
     *
     * @param task      the appointment task
     * @param startTime the start time
     * @param endTime   the end time
     * @return true if a slot with this time range exists
     */
    boolean existsByTaskAndStartTimeAndEndTime(AppointmentTask task, LocalTime startTime, LocalTime endTime);

    /**
     * Delete all slots for a task.
     *
     * @param task the appointment task
     */
    void deleteByTask(AppointmentTask task);

    /**
     * Delete all slots for a task by task ID.
     *
     * @param taskId the appointment task ID
     */
    void deleteByTaskId(Long taskId);

    /**
     * Find fully booked slots for a task.
     *
     * @param task the appointment task
     * @return list of fully booked slots
     */
    @Query("SELECT s FROM AppointmentSlot s WHERE s.task = :task AND s.bookedCount >= s.capacity")
    List<AppointmentSlot> findFullyBookedSlotsByTask(@Param("task") AppointmentTask task);
}
