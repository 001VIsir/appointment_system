package org.example.appointment_system.service;

import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.response.BookingResponse;
import org.springframework.stereotype.Service;

/**
 * 通知服务类（简化版）。
 *
 * <p>原用于通过WebSocket发送实时通知，现已简化为仅记录日志。
 * 用户可通过刷新页面查看最新预约状态。</p>
 *
 * <p>如需恢复实时通知功能：</p>
 * <ul>
 *   <li>添加 spring-boot-starter-websocket 依赖</li>
 *   <li>恢复 WebSocket 配置类</li>
 *   <li>使用 SimpMessagingTemplate 发送消息</li>
 * </ul>
 */
@Service
@Slf4j
public class NotificationService {

    // 通知类型常量（保留以兼容现有代码）
    public static final String TYPE_NEW_BOOKING = "NEW_BOOKING";
    public static final String TYPE_BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String TYPE_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String TYPE_BOOKING_COMPLETED = "BOOKING_COMPLETED";
    public static final String TYPE_BOOKING_REMINDER = "BOOKING_REMINDER";

    /**
     * 通知商户有新预约（简化版：仅记录日志）。
     *
     * @param booking the booking response
     */
    public void notifyNewBooking(BookingResponse booking) {
        if (booking.getMerchantId() == null) {
            log.warn("Cannot send new booking notification - merchantId is null");
            return;
        }

        log.info("NEW_BOOKING notification (简化模式): bookingId={}, merchantId={}, user={}",
            booking.getId(), booking.getMerchantId(), booking.getUsername());
    }

    /**
     * 通知预约已取消（简化版：仅记录日志）。
     *
     * @param booking the booking response
     */
    public void notifyBookingCancelled(BookingResponse booking) {
        log.info("BOOKING_CANCELLED notification (简化模式): bookingId={}", booking.getId());
    }

    /**
     * 通知用户预约已确认（简化版：仅记录日志）。
     *
     * @param booking the booking response
     */
    public void notifyBookingConfirmed(BookingResponse booking) {
        if (booking.getUserId() == null) {
            log.warn("Cannot send confirmation notification - userId is null");
            return;
        }

        log.info("BOOKING_CONFIRMED notification (简化模式): bookingId={}, userId={}",
            booking.getId(), booking.getUserId());
    }

    /**
     * 通知用户预约已完成（简化版：仅记录日志）。
     *
     * @param booking the booking response
     */
    public void notifyBookingCompleted(BookingResponse booking) {
        if (booking.getUserId() == null) {
            log.warn("Cannot send completion notification - userId is null");
            return;
        }

        log.info("BOOKING_COMPLETED notification (简化模式): bookingId={}, userId={}",
            booking.getId(), booking.getUserId());
    }

    /**
     * 发送预约提醒（简化版：仅记录日志）。
     *
     * @param booking the booking response
     * @param hoursUntilAppointment hours until the appointment
     */
    public void notifyBookingReminder(BookingResponse booking, int hoursUntilAppointment) {
        if (booking.getUserId() == null) {
            log.warn("Cannot send reminder notification - userId is null");
            return;
        }

        log.info("BOOKING_REMINDER notification (简化模式): bookingId={}, userId={}, hoursUntil={}",
            booking.getId(), booking.getUserId(), hoursUntilAppointment);
    }

    /**
     * 向商户发送自定义通知（简化版：仅记录日志）。
     *
     * @param merchantId the merchant ID
     * @param type the notification type
     * @param message the message content
     */
    public void notifyMerchant(Long merchantId, String type, String message) {
        log.info("Notification to merchant {}: {} - {}", merchantId, type, message);
    }

    /**
     * 向用户发送自定义通知（简化版：仅记录日志）。
     *
     * @param userId the user ID
     * @param type the notification type
     * @param message the message content
     */
    public void notifyUser(Long userId, String type, String message) {
        log.info("Notification to user {}: {} - {}", userId, type, message);
    }
}
