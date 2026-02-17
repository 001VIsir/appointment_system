package org.example.appointment_system.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for recording custom booking metrics.
 *
 * <p>This service provides methods to record various booking-related metrics
 * that are exposed via Prometheus for monitoring and alerting.</p>
 *
 * <h3>Recorded Metrics:</h3>
 * <ul>
 *   <li>{@code appointment.bookings.created} - Incremented when a booking is created</li>
 *   <li>{@code appointment.bookings.cancelled} - Incremented when a booking is cancelled</li>
 *   <li>{@code appointment.bookings.completed} - Incremented when a booking is completed</li>
 *   <li>{@code appointment.bookings.confirmed} - Incremented when a booking is confirmed</li>
 *   <li>{@code appointment.bookings.duration} - Timer for booking operation duration</li>
 *   <li>{@code appointment.errors} - Counter for errors by type</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingMetricsService {

    private final MeterRegistry meterRegistry;

    @Value("${app.metrics.booking.enabled:true}")
    private boolean metricsEnabled;

    /**
     * Record a booking creation event.
     *
     * @param merchantId the merchant ID
     * @param serviceId  the service item ID
     * @param taskId     the appointment task ID
     */
    public void recordBookingCreated(Long merchantId, Long serviceId, Long taskId) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.bookings.created")
                    .description("Count of bookings created")
                    .tag("merchant_id", String.valueOf(merchantId))
                    .tag("service_id", String.valueOf(serviceId))
                    .tag("task_id", String.valueOf(taskId))
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded booking created metric for merchant={}, service={}, task={}",
                    merchantId, serviceId, taskId);
        } catch (Exception e) {
            log.warn("Failed to record booking created metric", e);
        }
    }

    /**
     * Record a booking cancellation event.
     *
     * @param merchantId the merchant ID
     * @param reason     the cancellation reason (user or merchant)
     */
    public void recordBookingCancelled(Long merchantId, String reason) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.bookings.cancelled")
                    .description("Count of bookings cancelled")
                    .tag("merchant_id", String.valueOf(merchantId))
                    .tag("reason", reason != null ? reason : "unknown")
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded booking cancelled metric for merchant={}, reason={}", merchantId, reason);
        } catch (Exception e) {
            log.warn("Failed to record booking cancelled metric", e);
        }
    }

    /**
     * Record a booking completion event.
     *
     * @param merchantId the merchant ID
     */
    public void recordBookingCompleted(Long merchantId) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.bookings.completed")
                    .description("Count of bookings completed")
                    .tag("merchant_id", String.valueOf(merchantId))
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded booking completed metric for merchant={}", merchantId);
        } catch (Exception e) {
            log.warn("Failed to record booking completed metric", e);
        }
    }

    /**
     * Record a booking confirmation event.
     *
     * @param merchantId the merchant ID
     */
    public void recordBookingConfirmed(Long merchantId) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.bookings.confirmed")
                    .description("Count of bookings confirmed")
                    .tag("merchant_id", String.valueOf(merchantId))
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded booking confirmed metric for merchant={}", merchantId);
        } catch (Exception e) {
            log.warn("Failed to record booking confirmed metric", e);
        }
    }

    /**
     * Record a booking operation duration.
     *
     * @param operation the operation type (create, cancel, confirm, complete)
     * @param durationMs the duration in milliseconds
     */
    public void recordBookingDuration(String operation, long durationMs) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Timer.builder("appointment.bookings.duration")
                    .description("Duration of booking operations")
                    .tag("operation", operation)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);

            log.debug("Recorded booking duration metric: operation={}, duration={}ms", operation, durationMs);
        } catch (Exception e) {
            log.warn("Failed to record booking duration metric", e);
        }
    }

    /**
     * Record an error event.
     *
     * @param errorType  the error type
     * @param statusCode the HTTP status code
     * @param endpoint   the endpoint where the error occurred
     */
    public void recordError(String errorType, int statusCode, String endpoint) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.errors")
                    .description("Count of errors")
                    .tag("error_type", errorType)
                    .tag("status_code", String.valueOf(statusCode))
                    .tag("endpoint", endpoint)
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded error metric: type={}, status={}, endpoint={}", errorType, statusCode, endpoint);
        } catch (Exception e) {
            log.warn("Failed to record error metric", e);
        }
    }

    /**
     * Record a signed link access event.
     *
     * @param taskId   the task ID
     * @param valid    whether the link was valid
     */
    public void recordSignedLinkAccess(Long taskId, boolean valid) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.signed_links.access")
                    .description("Count of signed link accesses")
                    .tag("task_id", String.valueOf(taskId))
                    .tag("valid", String.valueOf(valid))
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded signed link access metric: task={}, valid={}", taskId, valid);
        } catch (Exception e) {
            log.warn("Failed to record signed link access metric", e);
        }
    }

    /**
     * Record a WebSocket notification event.
     *
     * @param notificationType the notification type
     * @param success          whether the notification was sent successfully
     */
    public void recordWebSocketNotification(String notificationType, boolean success) {
        if (!metricsEnabled) {
            return;
        }

        try {
            Counter counter = Counter.builder("appointment.websocket.notifications")
                    .description("Count of WebSocket notifications")
                    .tag("type", notificationType)
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry);

            counter.increment();
            log.debug("Recorded WebSocket notification metric: type={}, success={}", notificationType, success);
        } catch (Exception e) {
            log.warn("Failed to record WebSocket notification metric", e);
        }
    }
}
