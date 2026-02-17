package org.example.appointment_system.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Business Logger
 *
 * Provides structured logging for business events with MDC context.
 * Each business event is logged with:
 * - Event type
 * - Event data (as key-value pairs)
 * - Current trace context (TraceId, UserId, etc.)
 *
 * Usage:
 * <pre>
 * BusinessLogger.logBookingCreated(bookingId, userId, serviceId);
 * BusinessLogger.logBookingCancelled(bookingId, reason);
 * </pre>
 */
public final class BusinessLogger {

    private static final Logger BUSINESS_LOG = LoggerFactory.getLogger("BUSINESS_LOG");

    // Event types
    public static final String EVENT_USER_REGISTERED = "USER_REGISTERED";
    public static final String EVENT_USER_LOGIN = "USER_LOGIN";
    public static final String EVENT_USER_LOGOUT = "USER_LOGOUT";
    public static final String EVENT_MERCHANT_PROFILE_CREATED = "MERCHANT_PROFILE_CREATED";
    public static final String EVENT_MERCHANT_PROFILE_UPDATED = "MERCHANT_PROFILE_UPDATED";
    public static final String EVENT_SERVICE_ITEM_CREATED = "SERVICE_ITEM_CREATED";
    public static final String EVENT_SERVICE_ITEM_UPDATED = "SERVICE_ITEM_UPDATED";
    public static final String EVENT_SERVICE_ITEM_DELETED = "SERVICE_ITEM_DELETED";
    public static final String EVENT_TASK_CREATED = "APPOINTMENT_TASK_CREATED";
    public static final String EVENT_TASK_UPDATED = "APPOINTMENT_TASK_UPDATED";
    public static final String EVENT_SLOT_CREATED = "APPOINTMENT_SLOT_CREATED";
    public static final String EVENT_BOOKING_CREATED = "BOOKING_CREATED";
    public static final String EVENT_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String EVENT_BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String EVENT_BOOKING_COMPLETED = "BOOKING_COMPLETED";
    public static final String EVENT_BOOKING_TIMEOUT = "BOOKING_TIMEOUT";
    public static final String EVENT_SIGNED_LINK_GENERATED = "SIGNED_LINK_GENERATED";
    public static final String EVENT_SIGNED_LINK_ACCESSED = "SIGNED_LINK_ACCESSED";
    public static final String EVENT_SIGNED_LINK_EXPIRED = "SIGNED_LINK_EXPIRED";
    public static final String EVENT_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String EVENT_WEBSOCKET_NOTIFICATION = "WEBSOCKET_NOTIFICATION";

    private BusinessLogger() {
        // Utility class
    }

    // ==================== Generic Event Logging ====================

    /**
     * Log a business event with data
     */
    public static void logEvent(String eventType, Object... data) {
        StringBuilder message = new StringBuilder("[EVENT:").append(eventType).append("]");

        if (data != null && data.length > 0) {
            for (int i = 0; i < data.length; i += 2) {
                if (i + 1 < data.length) {
                    message.append(" ").append(data[i]).append("=").append(data[i + 1]);
                }
            }
        }

        BUSINESS_LOG.info(message.toString());
    }

    /**
     * Log a business event with warning level
     */
    public static void logWarning(String eventType, Object... data) {
        StringBuilder message = new StringBuilder("[EVENT:").append(eventType).append("]");

        if (data != null && data.length > 0) {
            for (int i = 0; i < data.length; i += 2) {
                if (i + 1 < data.length) {
                    message.append(" ").append(data[i]).append("=").append(data[i + 1]);
                }
            }
        }

        BUSINESS_LOG.warn(message.toString());
    }

    /**
     * Log a business event with error level
     */
    public static void logError(String eventType, String errorMessage, Object... data) {
        StringBuilder message = new StringBuilder("[EVENT:").append(eventType).append("]");
        message.append(" error=").append(errorMessage);

        if (data != null && data.length > 0) {
            for (int i = 0; i < data.length; i += 2) {
                if (i + 1 < data.length) {
                    message.append(" ").append(data[i]).append("=").append(data[i + 1]);
                }
            }
        }

        BUSINESS_LOG.error(message.toString());
    }

    // ==================== User Events ====================

    public static void logUserRegistered(Long userId, String username, String email) {
        logEvent(EVENT_USER_REGISTERED,
                "userId", userId,
                "username", username,
                "email", maskEmail(email));
    }

    public static void logUserLogin(Long userId, String username, boolean success) {
        logEvent(EVENT_USER_LOGIN,
                "userId", userId,
                "username", username,
                "success", success);
    }

    public static void logUserLogout(Long userId, String username) {
        logEvent(EVENT_USER_LOGOUT,
                "userId", userId,
                "username", username);
    }

    // ==================== Merchant Events ====================

    public static void logMerchantProfileCreated(Long merchantId, Long userId, String businessName) {
        logEvent(EVENT_MERCHANT_PROFILE_CREATED,
                "merchantId", merchantId,
                "userId", userId,
                "businessName", businessName);
    }

    public static void logMerchantProfileUpdated(Long merchantId, String businessName) {
        logEvent(EVENT_MERCHANT_PROFILE_UPDATED,
                "merchantId", merchantId,
                "businessName", businessName);
    }

    // ==================== Service Item Events ====================

    public static void logServiceItemCreated(Long serviceId, Long merchantId, String name, String category) {
        logEvent(EVENT_SERVICE_ITEM_CREATED,
                "serviceId", serviceId,
                "merchantId", merchantId,
                "name", name,
                "category", category);
    }

    public static void logServiceItemUpdated(Long serviceId, Long merchantId, String name) {
        logEvent(EVENT_SERVICE_ITEM_UPDATED,
                "serviceId", serviceId,
                "merchantId", merchantId,
                "name", name);
    }

    public static void logServiceItemDeleted(Long serviceId, Long merchantId, String name) {
        logEvent(EVENT_SERVICE_ITEM_DELETED,
                "serviceId", serviceId,
                "merchantId", merchantId,
                "name", name);
    }

    // ==================== Appointment Task Events ====================

    public static void logTaskCreated(Long taskId, Long serviceId, String taskDate, int totalCapacity) {
        logEvent(EVENT_TASK_CREATED,
                "taskId", taskId,
                "serviceId", serviceId,
                "taskDate", taskDate,
                "totalCapacity", totalCapacity);
    }

    public static void logTaskUpdated(Long taskId, String taskDate, int totalCapacity) {
        logEvent(EVENT_TASK_UPDATED,
                "taskId", taskId,
                "taskDate", taskDate,
                "totalCapacity", totalCapacity);
    }

    public static void logSlotCreated(Long slotId, Long taskId, String startTime, String endTime, int capacity) {
        logEvent(EVENT_SLOT_CREATED,
                "slotId", slotId,
                "taskId", taskId,
                "startTime", startTime,
                "endTime", endTime,
                "capacity", capacity);
    }

    // ==================== Booking Events ====================

    public static void logBookingCreated(Long bookingId, Long userId, Long slotId, Long taskId) {
        logEvent(EVENT_BOOKING_CREATED,
                "bookingId", bookingId,
                "userId", userId,
                "slotId", slotId,
                "taskId", taskId);
    }

    public static void logBookingConfirmed(Long bookingId, Long userId, Long merchantId) {
        logEvent(EVENT_BOOKING_CONFIRMED,
                "bookingId", bookingId,
                "userId", userId,
                "merchantId", merchantId);
    }

    public static void logBookingCancelled(Long bookingId, Long userId, String reason) {
        logEvent(EVENT_BOOKING_CANCELLED,
                "bookingId", bookingId,
                "userId", userId,
                "reason", reason);
    }

    public static void logBookingCompleted(Long bookingId, Long userId, Long merchantId) {
        logEvent(EVENT_BOOKING_COMPLETED,
                "bookingId", bookingId,
                "userId", userId,
                "merchantId", merchantId);
    }

    public static void logBookingTimeout(Long bookingId, Long userId) {
        logWarning(EVENT_BOOKING_TIMEOUT,
                "bookingId", bookingId,
                "userId", userId);
    }

    // ==================== Signed Link Events ====================

    public static void logSignedLinkGenerated(Long taskId, Long merchantId, String expiresAt) {
        logEvent(EVENT_SIGNED_LINK_GENERATED,
                "taskId", taskId,
                "merchantId", merchantId,
                "expiresAt", expiresAt);
    }

    public static void logSignedLinkAccessed(Long taskId, String clientIp, boolean valid) {
        logEvent(EVENT_SIGNED_LINK_ACCESSED,
                "taskId", taskId,
                "clientIp", clientIp,
                "valid", valid);
    }

    public static void logSignedLinkExpired(Long taskId, String clientIp) {
        logWarning(EVENT_SIGNED_LINK_EXPIRED,
                "taskId", taskId,
                "clientIp", clientIp);
    }

    // ==================== Security Events ====================

    public static void logRateLimitExceeded(String clientIp, String requestUri, String limitType) {
        logWarning(EVENT_RATE_LIMIT_EXCEEDED,
                "clientIp", clientIp,
                "requestUri", requestUri,
                "limitType", limitType);
    }

    // ==================== WebSocket Events ====================

    public static void logWebSocketNotification(Long merchantId, Long bookingId, String notificationType) {
        logEvent(EVENT_WEBSOCKET_NOTIFICATION,
                "merchantId", merchantId,
                "bookingId", bookingId,
                "notificationType", notificationType);
    }

    // ==================== Helper Methods ====================

    /**
     * Mask email for privacy
     */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
