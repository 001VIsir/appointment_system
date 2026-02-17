package org.example.appointment_system.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BookingMetricsService}.
 */
class BookingMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private BookingMetricsService bookingMetricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        bookingMetricsService = new BookingMetricsService(meterRegistry);
        ReflectionTestUtils.setField(bookingMetricsService, "metricsEnabled", true);
    }

    @Nested
    @DisplayName("Booking Created Metrics Tests")
    class BookingCreatedTests {

        @Test
        @DisplayName("Should record booking created event")
        void recordBookingCreated_shouldIncrementCounter() {
            bookingMetricsService.recordBookingCreated(1L, 2L, 3L);

            Counter counter = meterRegistry.find("appointment.bookings.created").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should include correct tags in booking created metric")
        void recordBookingCreated_shouldIncludeCorrectTags() {
            bookingMetricsService.recordBookingCreated(10L, 20L, 30L);

            Counter counter = meterRegistry.find("appointment.bookings.created").counter();
            assertNotNull(counter);
            assertEquals("10", counter.getId().getTag("merchant_id"));
            assertEquals("20", counter.getId().getTag("service_id"));
            assertEquals("30", counter.getId().getTag("task_id"));
        }

        @Test
        @DisplayName("Should accumulate multiple booking created events")
        void recordBookingCreated_shouldAccumulate() {
            bookingMetricsService.recordBookingCreated(1L, 2L, 3L);
            bookingMetricsService.recordBookingCreated(1L, 2L, 3L);
            bookingMetricsService.recordBookingCreated(1L, 2L, 3L);

            Counter counter = meterRegistry.find("appointment.bookings.created").counter();
            assertNotNull(counter);
            assertEquals(3.0, counter.count());
        }
    }

    @Nested
    @DisplayName("Booking Cancelled Metrics Tests")
    class BookingCancelledTests {

        @Test
        @DisplayName("Should record booking cancelled event")
        void recordBookingCancelled_shouldIncrementCounter() {
            bookingMetricsService.recordBookingCancelled(1L, "user_request");

            Counter counter = meterRegistry.find("appointment.bookings.cancelled").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should include correct reason tag")
        void recordBookingCancelled_shouldIncludeReasonTag() {
            bookingMetricsService.recordBookingCancelled(5L, "merchant_cancelled");

            Counter counter = meterRegistry.find("appointment.bookings.cancelled").counter();
            assertNotNull(counter);
            assertEquals("merchant_cancelled", counter.getId().getTag("reason"));
        }

        @Test
        @DisplayName("Should handle null reason")
        void recordBookingCancelled_shouldHandleNullReason() {
            bookingMetricsService.recordBookingCancelled(1L, null);

            Counter counter = meterRegistry.find("appointment.bookings.cancelled").counter();
            assertNotNull(counter);
            assertEquals("unknown", counter.getId().getTag("reason"));
        }
    }

    @Nested
    @DisplayName("Booking Completed Metrics Tests")
    class BookingCompletedTests {

        @Test
        @DisplayName("Should record booking completed event")
        void recordBookingCompleted_shouldIncrementCounter() {
            bookingMetricsService.recordBookingCompleted(1L);

            Counter counter = meterRegistry.find("appointment.bookings.completed").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should include merchant_id tag")
        void recordBookingCompleted_shouldIncludeMerchantIdTag() {
            bookingMetricsService.recordBookingCompleted(99L);

            Counter counter = meterRegistry.find("appointment.bookings.completed").counter();
            assertNotNull(counter);
            assertEquals("99", counter.getId().getTag("merchant_id"));
        }
    }

    @Nested
    @DisplayName("Booking Confirmed Metrics Tests")
    class BookingConfirmedTests {

        @Test
        @DisplayName("Should record booking confirmed event")
        void recordBookingConfirmed_shouldIncrementCounter() {
            bookingMetricsService.recordBookingConfirmed(1L);

            Counter counter = meterRegistry.find("appointment.bookings.confirmed").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should include merchant_id tag")
        void recordBookingConfirmed_shouldIncludeMerchantIdTag() {
            bookingMetricsService.recordBookingConfirmed(42L);

            Counter counter = meterRegistry.find("appointment.bookings.confirmed").counter();
            assertNotNull(counter);
            assertEquals("42", counter.getId().getTag("merchant_id"));
        }
    }

    @Nested
    @DisplayName("Booking Duration Metrics Tests")
    class BookingDurationTests {

        @Test
        @DisplayName("Should record booking duration")
        void recordBookingDuration_shouldRecordTimer() {
            bookingMetricsService.recordBookingDuration("create", 150);

            Timer timer = meterRegistry.find("appointment.bookings.duration").timer();
            assertNotNull(timer);
            assertEquals(1, timer.count());
            // Duration is recorded in milliseconds, check total time in seconds
            assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 150);
        }

        @Test
        @DisplayName("Should include operation tag")
        void recordBookingDuration_shouldIncludeOperationTag() {
            bookingMetricsService.recordBookingDuration("cancel", 50);

            Timer timer = meterRegistry.find("appointment.bookings.duration").timer();
            assertNotNull(timer);
            assertEquals("cancel", timer.getId().getTag("operation"));
        }

        @Test
        @DisplayName("Should accumulate multiple duration recordings")
        void recordBookingDuration_shouldAccumulate() {
            bookingMetricsService.recordBookingDuration("create", 100);
            bookingMetricsService.recordBookingDuration("create", 200);
            bookingMetricsService.recordBookingDuration("create", 300);

            Timer timer = meterRegistry.find("appointment.bookings.duration").timer();
            assertNotNull(timer);
            assertEquals(3, timer.count());
        }
    }

    @Nested
    @DisplayName("Error Metrics Tests")
    class ErrorMetricsTests {

        @Test
        @DisplayName("Should record error event")
        void recordError_shouldIncrementCounter() {
            bookingMetricsService.recordError("validation_error", 400, "/api/bookings");

            Counter counter = meterRegistry.find("appointment.errors").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should include correct error tags")
        void recordError_shouldIncludeCorrectTags() {
            bookingMetricsService.recordError("not_found", 404, "/api/bookings/999");

            Counter counter = meterRegistry.find("appointment.errors").counter();
            assertNotNull(counter);
            assertEquals("not_found", counter.getId().getTag("error_type"));
            assertEquals("404", counter.getId().getTag("status_code"));
            assertEquals("/api/bookings/999", counter.getId().getTag("endpoint"));
        }
    }

    @Nested
    @DisplayName("Signed Link Metrics Tests")
    class SignedLinkMetricsTests {

        @Test
        @DisplayName("Should record valid signed link access")
        void recordSignedLinkAccess_shouldRecordValidAccess() {
            bookingMetricsService.recordSignedLinkAccess(1L, true);

            Counter counter = meterRegistry.find("appointment.signed_links.access").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
            assertEquals("true", counter.getId().getTag("valid"));
        }

        @Test
        @DisplayName("Should record invalid signed link access")
        void recordSignedLinkAccess_shouldRecordInvalidAccess() {
            bookingMetricsService.recordSignedLinkAccess(1L, false);

            Counter counter = meterRegistry.find("appointment.signed_links.access").counter();
            assertNotNull(counter);
            assertEquals("false", counter.getId().getTag("valid"));
        }
    }

    @Nested
    @DisplayName("WebSocket Notification Metrics Tests")
    class WebSocketNotificationTests {

        @Test
        @DisplayName("Should record successful WebSocket notification")
        void recordWebSocketNotification_shouldRecordSuccess() {
            bookingMetricsService.recordWebSocketNotification("booking_created", true);

            Counter counter = meterRegistry.find("appointment.websocket.notifications").counter();
            assertNotNull(counter);
            assertEquals(1.0, counter.count());
            assertEquals("true", counter.getId().getTag("success"));
        }

        @Test
        @DisplayName("Should record failed WebSocket notification")
        void recordWebSocketNotification_shouldRecordFailure() {
            bookingMetricsService.recordWebSocketNotification("booking_cancelled", false);

            Counter counter = meterRegistry.find("appointment.websocket.notifications").counter();
            assertNotNull(counter);
            assertEquals("false", counter.getId().getTag("success"));
        }

        @Test
        @DisplayName("Should include notification type tag")
        void recordWebSocketNotification_shouldIncludeTypeTag() {
            bookingMetricsService.recordWebSocketNotification("booking_reminder", true);

            Counter counter = meterRegistry.find("appointment.websocket.notifications").counter();
            assertNotNull(counter);
            assertEquals("booking_reminder", counter.getId().getTag("type"));
        }
    }

    @Nested
    @DisplayName("Metrics Disabled Tests")
    class MetricsDisabledTests {

        @Test
        @DisplayName("Should not record metrics when disabled")
        void whenMetricsDisabled_shouldNotRecord() {
            ReflectionTestUtils.setField(bookingMetricsService, "metricsEnabled", false);

            bookingMetricsService.recordBookingCreated(1L, 2L, 3L);
            bookingMetricsService.recordBookingCancelled(1L, "test");
            bookingMetricsService.recordBookingCompleted(1L);
            bookingMetricsService.recordBookingConfirmed(1L);
            bookingMetricsService.recordBookingDuration("create", 100);
            bookingMetricsService.recordError("test", 500, "/test");
            bookingMetricsService.recordSignedLinkAccess(1L, true);
            bookingMetricsService.recordWebSocketNotification("test", true);

            // No metrics should be recorded
            assertEquals(0, meterRegistry.getMeters().size());
        }
    }
}
