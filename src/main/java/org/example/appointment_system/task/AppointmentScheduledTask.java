package org.example.appointment_system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.service.StatisticsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled tasks for appointment management.
 *
 * <p>This class contains various scheduled tasks that run automatically:</p>
 * <ul>
 *   <li>Appointment timeout handling - cancels bookings for past slots</li>
 *   <li>Appointment reminders - prepares reminder notifications</li>
 *   <li>Auto-completion - marks completed appointments</li>
 *   <li>Daily statistics - generates daily summary reports</li>
 * </ul>
 *
 * <h3>Schedule:</h3>
 * <ul>
 *   <li>Timeout task: Every minute</li>
 *   <li>Reminder task: Every 5 minutes</li>
 *   <li>Auto-completion: Every hour</li>
 *   <li>Daily statistics: Daily at 1:00 AM</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentScheduledTask {

    private final BookingRepository bookingRepository;
    private final AppointmentTaskRepository taskRepository;
    private final AppointmentSlotRepository slotRepository;
    private final StatisticsService statisticsService;

    // Configuration constants
    private static final int REMINDER_HOURS_BEFORE = 24; // Send reminder 24 hours before
    private static final int AUTO_COMPLETE_HOURS_AFTER = 2; // Auto-complete 2 hours after end time

    // ============================================
    // Timeout Handling Task (Every Minute)
    // ============================================

    /**
     * Handle booking timeouts.
     * Cancels bookings for slots that have ended.
     *
     * <p>This task runs every minute to check for expired slots
     * and cancel any remaining active bookings.</p>
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void handleBookingTimeouts() {
        log.debug("Running booking timeout check");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalTime currentTime = now.toLocalTime();

            // Find all active tasks for today
            List<AppointmentTask> todayTasks = taskRepository.findByTaskDateAndActiveTrue(today);

            int cancelledCount = 0;

            for (AppointmentTask task : todayTasks) {
                // Get all slots for this task that have ended
                List<AppointmentSlot> endedSlots = slotRepository.findByTask(task).stream()
                        .filter(slot -> slot.getEndTime().isBefore(currentTime))
                        .toList();

                for (AppointmentSlot slot : endedSlots) {
                    // Cancel any active bookings for ended slots
                    List<Booking> activeBookings = bookingRepository.findBySlotIdAndStatus(
                            slot.getId(), BookingStatus.PENDING);

                    for (Booking booking : activeBookings) {
                        // Cancel the booking
                        booking.cancel();
                        bookingRepository.save(booking);

                        // Decrement the slot's booked count
                        slot.decrementBookedCount();
                        slotRepository.save(slot);

                        cancelledCount++;
                        log.info("Cancelled expired booking {} for slot {} (task {})",
                                booking.getId(), slot.getId(), task.getId());
                    }
                }
            }

            if (cancelledCount > 0) {
                log.info("Booking timeout check completed: {} bookings cancelled", cancelledCount);
            }

        } catch (Exception e) {
            log.error("Error during booking timeout check", e);
        }
    }

    // ============================================
    // Reminder Task (Every 5 Minutes)
    // ============================================

    /**
     * Send appointment reminders.
     * Identifies bookings that need reminders and prepares notifications.
     *
     * <p>This task runs every 5 minutes to check for upcoming appointments
     * and queue reminder notifications.</p>
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void sendAppointmentReminders() {
        log.debug("Running appointment reminder check");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderThreshold = now.plusHours(REMINDER_HOURS_BEFORE);

            LocalDate targetDate = reminderThreshold.toLocalDate();
            LocalTime targetTimeStart = reminderThreshold.toLocalTime();
            LocalTime targetTimeEnd = targetTimeStart.plusMinutes(5);

            // Find all active tasks for the reminder window
            List<AppointmentTask> upcomingTasks = taskRepository.findByTaskDateAndActiveTrue(targetDate);

            int reminderCount = 0;

            for (AppointmentTask task : upcomingTasks) {
                // Find slots in the reminder window
                List<AppointmentSlot> upcomingSlots = slotRepository.findByTaskAndTimeRange(
                        task, targetTimeStart, targetTimeEnd);

                for (AppointmentSlot slot : upcomingSlots) {
                    // Get confirmed bookings for this slot
                    List<Booking> confirmedBookings = bookingRepository.findBySlotIdAndStatus(
                            slot.getId(), BookingStatus.CONFIRMED);

                    for (Booking booking : confirmedBookings) {
                        // In a real system, you would queue a notification here
                        // For now, we just log it
                        log.info("Reminder: Booking {} for user {} is scheduled for {} at {}",
                                booking.getId(),
                                booking.getUser().getId(),
                                task.getTaskDate(),
                                slot.getStartTime());

                        // TODO: Integrate with notification service (email, push, etc.)
                        reminderCount++;
                    }
                }
            }

            if (reminderCount > 0) {
                log.info("Appointment reminder check completed: {} reminders queued", reminderCount);
            }

        } catch (Exception e) {
            log.error("Error during appointment reminder check", e);
        }
    }

    // ============================================
    // Auto-Completion Task (Every Hour)
    // ============================================

    /**
     * Auto-complete appointments.
     * Marks appointments as completed after they have ended.
     *
     * <p>This task runs every hour to automatically complete confirmed bookings
     * for slots that ended more than AUTO_COMPLETE_HOURS_AFTER hours ago.</p>
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void autoCompleteAppointments() {
        log.debug("Running auto-completion check");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime completionThreshold = now.minusHours(AUTO_COMPLETE_HOURS_AFTER);

            LocalDate cutoffDate = completionThreshold.toLocalDate();
            LocalTime cutoffTime = completionThreshold.toLocalTime();

            // Find all tasks before or on the cutoff date
            List<AppointmentTask> pastTasks = taskRepository.findByTaskDateBetweenAndActiveTrue(
                    cutoffDate.minusDays(7), cutoffDate);

            int completedCount = 0;

            for (AppointmentTask task : pastTasks) {
                // For tasks on the cutoff date, check time
                if (task.getTaskDate().isEqual(cutoffDate)) {
                    // Get slots that ended before the cutoff time
                    List<AppointmentSlot> completedSlots = slotRepository.findByTask(task).stream()
                            .filter(slot -> slot.getEndTime().isBefore(cutoffTime))
                            .toList();

                    for (AppointmentSlot slot : completedSlots) {
                        // Complete confirmed bookings
                        List<Booking> confirmedBookings = bookingRepository.findBySlotIdAndStatus(
                                slot.getId(), BookingStatus.CONFIRMED);

                        for (Booking booking : confirmedBookings) {
                            booking.complete();
                            bookingRepository.save(booking);

                            completedCount++;
                            log.info("Auto-completed booking {} for slot {} (task {})",
                                    booking.getId(), slot.getId(), task.getId());
                        }
                    }
                } else {
                    // For tasks before the cutoff date, all slots should be completed
                    List<AppointmentSlot> allSlots = slotRepository.findByTask(task);

                    for (AppointmentSlot slot : allSlots) {
                        List<Booking> confirmedBookings = bookingRepository.findBySlotIdAndStatus(
                                slot.getId(), BookingStatus.CONFIRMED);

                        for (Booking booking : confirmedBookings) {
                            booking.complete();
                            bookingRepository.save(booking);

                            completedCount++;
                            log.info("Auto-completed booking {} for slot {} (task {})",
                                    booking.getId(), slot.getId(), task.getId());
                        }
                    }
                }
            }

            if (completedCount > 0) {
                log.info("Auto-completion check completed: {} bookings marked as completed", completedCount);
            }

        } catch (Exception e) {
            log.error("Error during auto-completion check", e);
        }
    }

    // ============================================
    // Daily Statistics Task (Daily at 1:00 AM)
    // ============================================

    /**
     * Generate daily statistics.
     * Creates summary reports for the previous day.
     *
     * <p>This task runs daily at 1:00 AM to generate statistics
     * for the previous day and store them for reporting.</p>
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1:00 AM
    @Transactional
    public void generateDailyStatistics() {
        log.info("Running daily statistics generation");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            // Generate and store daily summary
            statisticsService.generateDailySummary(yesterday);

            // Log daily metrics
            logDailyMetrics(yesterday);

            log.info("Daily statistics generation completed for {}", yesterday);

        } catch (Exception e) {
            log.error("Error during daily statistics generation", e);
        }
    }

    /**
     * Log daily metrics for monitoring.
     *
     * @param date the date to log metrics for
     */
    private void logDailyMetrics(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        // Get daily booking counts
        List<Booking> dayBookings = bookingRepository.findByCreatedAtBetween(dayStart, dayEnd);
        long totalBookings = dayBookings.size();
        long activeBookings = dayBookings.stream().filter(Booking::isActive).count();
        long completedBookings = dayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long cancelledBookings = dayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        // Get task counts
        List<AppointmentTask> dayTasks = taskRepository.findByTaskDate(date);
        long totalTasks = dayTasks.size();
        long activeTasks = dayTasks.stream().filter(AppointmentTask::isActive).count();

        log.info("Daily metrics for {}: " +
                        "bookings(total={}, active={}, completed={}, cancelled={}), " +
                        "tasks(total={}, active={})",
                date, totalBookings, activeBookings, completedBookings, cancelledBookings,
                totalTasks, activeTasks);

        // Calculate and log utilization rate
        if (totalTasks > 0) {
            int totalCapacity = 0;
            int totalBooked = 0;

            for (AppointmentTask task : dayTasks) {
                totalCapacity += slotRepository.sumCapacityByTaskId(task.getId());
                totalBooked += slotRepository.sumBookedCountByTaskId(task.getId());
            }

            double utilizationRate = totalCapacity > 0
                    ? (double) totalBooked / totalCapacity * 100 : 0;

            log.info("Daily utilization for {}: capacity={}, booked={}, rate={}%",
                    date, totalCapacity, totalBooked, String.format("%.2f", utilizationRate));
        }
    }

    // ============================================
    // Cleanup Task (Daily at 2:00 AM)
    // ============================================

    /**
     * Cleanup old data.
     * Removes or archives old data to keep the system efficient.
     *
     * <p>This task runs daily at 2:00 AM to clean up:</p>
     * <ul>
     *   <li>Old completed/cancelled bookings (archive to history)</li>
     *   <li>Old inactive tasks</li>
     *   <li>Expired Redis keys</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2:00 AM
    @Transactional
    public void cleanupOldData() {
        log.info("Running data cleanup");

        try {
            LocalDate archiveDate = LocalDate.now().minusDays(90); // Keep 90 days

            // Find old cancelled/completed bookings
            LocalDateTime archiveThreshold = archiveDate.atStartOfDay();

            // In a real system, you would:
            // 1. Archive old bookings to a history table
            // 2. Delete old Redis keys
            // 3. Compress log files
            // For now, we just log what would be cleaned up

            List<Booking> oldCancelledBookings = bookingRepository.findByStatusIn(
                    List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED)).stream()
                    .filter(b -> b.getCreatedAt().isBefore(archiveThreshold))
                    .toList();

            if (!oldCancelledBookings.isEmpty()) {
                log.info("Would archive {} old bookings (before {})", oldCancelledBookings.size(), archiveDate);
                // In production: archiveService.archiveBookings(oldCancelledBookings);
            }

            log.info("Data cleanup completed");

        } catch (Exception e) {
            log.error("Error during data cleanup", e);
        }
    }
}
