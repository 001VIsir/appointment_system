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
 * 预约管理的定时任务。
 *
 * <p>此类包含各种自动运行的定时任务：</p>
 * <ul>
 *   <li>预约超时处理 - 取消过去时段的预约</li>
 *   <li>预约提醒 - 准备提醒通知</li>
 *   <li>自动完成 - 标记已完成的预约</li>
 *   <li>每日统计 - 生成每日汇总报告</li>
 * </ul>
 *
 * <h3>执行计划：</h3>
 * <ul>
 *   <li>超时任务：每分钟</li>
 *   <li>提醒任务：每5分钟</li>
 *   <li>自动完成：每小时</li>
 *   <li>每日统计：每天凌晨1点</li>
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

    // 配置常量
    private static final int REMINDER_HOURS_BEFORE = 24; // 提前24小时发送提醒
    private static final int AUTO_COMPLETE_HOURS_AFTER = 2; // 结束后2小时自动完成

    // ============================================
    // 超时处理任务（每分钟）
    // ============================================

    /**
     * 处理预约超时。
     * 取消已结束时段的预约。
     *
     * <p>此任务每分钟运行一次，检查已过期的时段
     * 并取消任何剩余的有效预约。</p>
     */
    @Scheduled(fixedRate = 60000) // 每分钟
    @Transactional
    public void handleBookingTimeouts() {
        log.debug("Running booking timeout check");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalTime currentTime = now.toLocalTime();

            // 查找今天的所有有效任务
            List<AppointmentTask> todayTasks = taskRepository.findByTaskDateAndActiveTrue(today);

            int cancelledCount = 0;

            for (AppointmentTask task : todayTasks) {
                // 获取此任务已结束的所有时段
                List<AppointmentSlot> endedSlots = slotRepository.findByTask(task).stream()
                        .filter(slot -> slot.getEndTime().isBefore(currentTime))
                        .toList();

                for (AppointmentSlot slot : endedSlots) {
                    // 取消已结束时段的任何有效预约
                    List<Booking> activeBookings = bookingRepository.findBySlotIdAndStatus(
                            slot.getId(), BookingStatus.PENDING);

                    for (Booking booking : activeBookings) {
                        // 取消预约
                        booking.cancel();
                        bookingRepository.save(booking);

                        // 减少时段的已预约数量
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
    // 提醒任务（每5分钟）
    // ============================================

    /**
     * 发送预约提醒。
     * 识别需要提醒的预约并准备通知。
     *
     * <p>此任务每5分钟运行一次，检查即将到来的预约
     * 并排队提醒通知。</p>
     */
    @Scheduled(fixedRate = 300000) // 每5分钟
    @Transactional
    public void sendAppointmentReminders() {
        log.debug("Running appointment reminder check");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderThreshold = now.plusHours(REMINDER_HOURS_BEFORE);

            LocalDate targetDate = reminderThreshold.toLocalDate();
            LocalTime targetTimeStart = reminderThreshold.toLocalTime();
            LocalTime targetTimeEnd = targetTimeStart.plusMinutes(5);

            // 查找提醒窗口内的所有有效任务
            List<AppointmentTask> upcomingTasks = taskRepository.findByTaskDateAndActiveTrue(targetDate);

            int reminderCount = 0;

            for (AppointmentTask task : upcomingTasks) {
                // 查找提醒窗口内的时段
                List<AppointmentSlot> upcomingSlots = slotRepository.findByTaskAndTimeRange(
                        task, targetTimeStart, targetTimeEnd);

                for (AppointmentSlot slot : upcomingSlots) {
                    // 获取此时段的已确认预约
                    List<Booking> confirmedBookings = bookingRepository.findBySlotIdAndStatus(
                            slot.getId(), BookingStatus.CONFIRMED);

                    for (Booking booking : confirmedBookings) {
                        // 在实际系统中，你会在此排队通知
                        // 目前，我们只记录日志
                        log.info("Reminder: Booking {} for user {} is scheduled for {} at {}",
                                booking.getId(),
                                booking.getUser().getId(),
                                task.getTaskDate(),
                                slot.getStartTime());

                        // TODO: 与通知服务集成（邮件、推送等）
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
    // 自动完成任务（每小时）
    // ============================================

    /**
     * 自动完成预约。
     * 在预约结束后标记为已完成。
     *
     * <p>此任务每小时运行一次，自动完成AUTO_COMPLETE_HOURS_AFTER小时前结束的
     * 时段的已确认预约。</p>
     */
    @Scheduled(fixedRate = 3600000) // 每小时
    @Transactional
    public void autoCompleteAppointments() {
        log.debug("Running auto-completion check");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime completionThreshold = now.minusHours(AUTO_COMPLETE_HOURS_AFTER);

            LocalDate cutoffDate = completionThreshold.toLocalDate();
            LocalTime cutoffTime = completionThreshold.toLocalTime();

            // 查找截止日期当天或之前的所有任务
            List<AppointmentTask> pastTasks = taskRepository.findByTaskDateBetweenAndActiveTrue(
                    cutoffDate.minusDays(7), cutoffDate);

            int completedCount = 0;

            for (AppointmentTask task : pastTasks) {
                // 对于截止日期当天的任务，检查时间
                if (task.getTaskDate().isEqual(cutoffDate)) {
                    // 获取截止时间前结束的时段
                    List<AppointmentSlot> completedSlots = slotRepository.findByTask(task).stream()
                            .filter(slot -> slot.getEndTime().isBefore(cutoffTime))
                            .toList();

                    for (AppointmentSlot slot : completedSlots) {
                        // 完成已确认的预约
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
                    // 对于截止日期之前的任务，所有时段都应该被标记为完成
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
    // 每日统计任务（每天凌晨1点）
    // ============================================

    /**
     * 生成每日统计。
     * 为前一天创建汇总报告。
     *
     * <p>此任务每天凌晨1点运行，为前一天生成统计
     * 数据并存储以供报告使用。</p>
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点
    @Transactional
    public void generateDailyStatistics() {
        log.info("Running daily statistics generation");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            // 生成并存储每日汇总
            statisticsService.generateDailySummary(yesterday);

            // 记录每日指标
            logDailyMetrics(yesterday);

            log.info("Daily statistics generation completed for {}", yesterday);

        } catch (Exception e) {
            log.error("Error during daily statistics generation", e);
        }
    }

    /**
     * 记录每日的监控指标。
     *
     * @param date 要记录指标的日期
     */
    private void logDailyMetrics(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        // 获取每日预约数量
        List<Booking> dayBookings = bookingRepository.findByCreatedAtBetween(dayStart, dayEnd);
        long totalBookings = dayBookings.size();
        long activeBookings = dayBookings.stream().filter(Booking::isActive).count();
        long completedBookings = dayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long cancelledBookings = dayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        // 获取任务数量
        List<AppointmentTask> dayTasks = taskRepository.findByTaskDate(date);
        long totalTasks = dayTasks.size();
        long activeTasks = dayTasks.stream().filter(AppointmentTask::isActive).count();

        log.info("Daily metrics for {}: " +
                        "bookings(total={}, active={}, completed={}, cancelled={}), " +
                        "tasks(total={}, active={})",
                date, totalBookings, activeBookings, completedBookings, cancelledBookings,
                totalTasks, activeTasks);

        // 计算并记录利用率
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
    // 清理任务（每天凌晨2点）
    // ============================================

    /**
     * 清理旧数据。
     * 删除或归档旧数据以保持系统高效。
     *
     * <p>此任务每天凌晨2点运行进行清理：</p>
     * <ul>
     *   <li>旧的已完成/已取消预约（归档到历史）</li>
     *   <li>旧的非活动任务</li>
     *   <li>过期的Redis键</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @Transactional
    public void cleanupOldData() {
        log.info("Running data cleanup");

        try {
            LocalDate archiveDate = LocalDate.now().minusDays(90); // 保留90天

            // 查找旧的已取消/已完成预约
            LocalDateTime archiveThreshold = archiveDate.atStartOfDay();

            // 在实际系统中，你应该：
            // 1. 将旧预约归档到历史表
            // 2. 删除旧的Redis键
            // 3. 压缩日志文件
            // 目前，我们只记录将要清理的内容

            List<Booking> oldCancelledBookings = bookingRepository.findByStatusIn(
                    List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED)).stream()
                    .filter(b -> b.getCreatedAt().isBefore(archiveThreshold))
                    .toList();

            if (!oldCancelledBookings.isEmpty()) {
                log.info("Would archive {} old bookings (before {})", oldCancelledBookings.size(), archiveDate);
                // 生产环境：archiveService.archiveBookings(oldCancelledBookings);
            }

            log.info("Data cleanup completed");

        } catch (Exception e) {
            log.error("Error during data cleanup", e);
        }
    }
}
