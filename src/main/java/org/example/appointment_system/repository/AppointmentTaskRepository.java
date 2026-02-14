package org.example.appointment_system.repository;

import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AppointmentTask entity.
 *
 * <p>Provides data access operations for appointment tasks including:</p>
 * <ul>
 *   <li>Standard CRUD operations (inherited from JpaRepository)</li>
 *   <li>Find by service item</li>
 *   <li>Find by date and active status</li>
 *   <li>Check existence by service and date</li>
 * </ul>
 */
@Repository
public interface AppointmentTaskRepository extends JpaRepository<AppointmentTask, Long> {

    /**
     * Find all appointment tasks belonging to a specific service.
     *
     * @param service the service item to search by
     * @return list of appointment tasks for the service
     */
    List<AppointmentTask> findByService(ServiceItem service);

    /**
     * Find all appointment tasks belonging to a specific service by service ID.
     *
     * @param serviceId the service item ID to search by
     * @return list of appointment tasks for the service
     */
    List<AppointmentTask> findByServiceId(Long serviceId);

    /**
     * Find all active appointment tasks belonging to a specific service.
     *
     * @param service the service item to search by
     * @return list of active appointment tasks for the service
     */
    List<AppointmentTask> findByServiceAndActiveTrue(ServiceItem service);

    /**
     * Find all active appointment tasks belonging to a specific service by service ID.
     *
     * @param serviceId the service item ID to search by
     * @return list of active appointment tasks for the service
     */
    List<AppointmentTask> findByServiceIdAndActiveTrue(Long serviceId);

    /**
     * Find an appointment task by ID and service.
     *
     * @param id      the appointment task ID
     * @param service the service item
     * @return Optional containing the task if found and belongs to the service
     */
    Optional<AppointmentTask> findByIdAndService(Long id, ServiceItem service);

    /**
     * Find an appointment task by ID and service ID.
     *
     * @param id        the appointment task ID
     * @param serviceId the service item ID
     * @return Optional containing the task if found
     */
    Optional<AppointmentTask> findByIdAndServiceId(Long id, Long serviceId);

    /**
     * Find all appointment tasks by date.
     *
     * @param taskDate the date to search by
     * @return list of appointment tasks on the specified date
     */
    List<AppointmentTask> findByTaskDate(LocalDate taskDate);

    /**
     * Find all active appointment tasks by date.
     *
     * @param taskDate the date to search by
     * @return list of active appointment tasks on the specified date
     */
    List<AppointmentTask> findByTaskDateAndActiveTrue(LocalDate taskDate);

    /**
     * Find all appointment tasks for a service on a specific date.
     *
     * @param service  the service item
     * @param taskDate the date to search by
     * @return list of appointment tasks matching the criteria
     */
    List<AppointmentTask> findByServiceAndTaskDate(ServiceItem service, LocalDate taskDate);

    /**
     * Find all appointment tasks for a service ID on a specific date.
     *
     * @param serviceId the service item ID
     * @param taskDate  the date to search by
     * @return list of appointment tasks matching the criteria
     */
    List<AppointmentTask> findByServiceIdAndTaskDate(Long serviceId, LocalDate taskDate);

    /**
     * Find all appointment tasks between two dates (inclusive).
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return list of appointment tasks in the date range
     */
    List<AppointmentTask> findByTaskDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find all active appointment tasks between two dates (inclusive).
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return list of active appointment tasks in the date range
     */
    List<AppointmentTask> findByTaskDateBetweenAndActiveTrue(LocalDate startDate, LocalDate endDate);

    /**
     * Find all active appointment tasks for a service between two dates.
     *
     * @param service   the service item
     * @param startDate the start date
     * @param endDate   the end date
     * @return list of active appointment tasks in the date range
     */
    List<AppointmentTask> findByServiceAndTaskDateBetweenAndActiveTrue(
            ServiceItem service, LocalDate startDate, LocalDate endDate);

    /**
     * Check if an appointment task exists for a service on a specific date.
     *
     * @param service  the service item
     * @param taskDate the date to check
     * @return true if a task exists for the service on the date
     */
    boolean existsByServiceAndTaskDate(ServiceItem service, LocalDate taskDate);

    /**
     * Check if an appointment task exists for a service ID on a specific date.
     *
     * @param serviceId the service item ID
     * @param taskDate  the date to check
     * @return true if a task exists for the service on the date
     */
    boolean existsByServiceIdAndTaskDate(Long serviceId, LocalDate taskDate);

    /**
     * Count all appointment tasks for a service.
     *
     * @param service the service item
     * @return the count of appointment tasks
     */
    long countByService(ServiceItem service);

    /**
     * Count active appointment tasks for a service.
     *
     * @param service the service item
     * @return the count of active appointment tasks
     */
    long countByServiceAndActiveTrue(ServiceItem service);

    /**
     * Find all active appointment tasks for a service with date >= specified date.
     *
     * @param service      the service item
     * @param taskDateFrom the minimum date (inclusive)
     * @return list of active appointment tasks
     */
    List<AppointmentTask> findByServiceAndTaskDateGreaterThanEqualAndActiveTrue(
            ServiceItem service, LocalDate taskDateFrom);

    /**
     * Find all active appointment tasks for a service ID with date >= specified date.
     *
     * @param serviceId    the service item ID
     * @param taskDateFrom the minimum date (inclusive)
     * @return list of active appointment tasks
     */
    List<AppointmentTask> findByServiceIdAndTaskDateGreaterThanEqualAndActiveTrue(
            Long serviceId, LocalDate taskDateFrom);

    /**
     * Find all inactive appointment tasks for a service.
     *
     * @param service the service item
     * @return list of inactive appointment tasks
     */
    List<AppointmentTask> findByServiceAndActiveFalse(ServiceItem service);

    /**
     * Find all public active tasks by ID (for signed link access).
     *
     * @param id     the appointment task ID
     * @param active the active status
     * @return Optional containing the task if found and active
     */
    Optional<AppointmentTask> findByIdAndActive(Long id, Boolean active);
}
