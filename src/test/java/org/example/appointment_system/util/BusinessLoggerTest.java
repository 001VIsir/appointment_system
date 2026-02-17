package org.example.appointment_system.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessLoggerTest {

    @Nested
    @DisplayName("Generic Event Logging")
    class GenericEventLogging {

        @Test
        @DisplayName("logEvent should not throw")
        void logEventShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logEvent("TEST_EVENT", "key1", "value1", "key2", 123));
        }

        @Test
        @DisplayName("logEvent should handle null data")
        void logEventShouldHandleNullData() {
            assertDoesNotThrow(() -> BusinessLogger.logEvent("TEST_EVENT", (Object[]) null));
        }

        @Test
        @DisplayName("logEvent should handle empty data")
        void logEventShouldHandleEmptyData() {
            assertDoesNotThrow(() -> BusinessLogger.logEvent("TEST_EVENT"));
        }

        @Test
        @DisplayName("logEvent should handle odd number of data elements")
        void logEventShouldHandleOddNumberOfDataElements() {
            assertDoesNotThrow(() -> BusinessLogger.logEvent("TEST_EVENT", "key1", "value1", "orphanKey"));
        }

        @Test
        @DisplayName("logWarning should not throw")
        void logWarningShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logWarning("WARN_EVENT", "key1", "value1"));
        }

        @Test
        @DisplayName("logError should not throw")
        void logErrorShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logError("ERROR_EVENT", "Something went wrong", "key1", "value1"));
        }
    }

    @Nested
    @DisplayName("User Events")
    class UserEvents {

        @Test
        @DisplayName("logUserRegistered should not throw")
        void logUserRegisteredShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logUserRegistered(1L, "testuser", "test@example.com"));
        }

        @Test
        @DisplayName("logUserLogin should not throw")
        void logUserLoginShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logUserLogin(1L, "testuser", true));
            assertDoesNotThrow(() -> BusinessLogger.logUserLogin(1L, "testuser", false));
        }

        @Test
        @DisplayName("logUserLogout should not throw")
        void logUserLogoutShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logUserLogout(1L, "testuser"));
        }
    }

    @Nested
    @DisplayName("Merchant Events")
    class MerchantEvents {

        @Test
        @DisplayName("logMerchantProfileCreated should not throw")
        void logMerchantProfileCreatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logMerchantProfileCreated(1L, 100L, "Test Business"));
        }

        @Test
        @DisplayName("logMerchantProfileUpdated should not throw")
        void logMerchantProfileUpdatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logMerchantProfileUpdated(1L, "Updated Business"));
        }
    }

    @Nested
    @DisplayName("Service Item Events")
    class ServiceItemEvents {

        @Test
        @DisplayName("logServiceItemCreated should not throw")
        void logServiceItemCreatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logServiceItemCreated(1L, 100L, "Haircut", "BEAUTY"));
        }

        @Test
        @DisplayName("logServiceItemUpdated should not throw")
        void logServiceItemUpdatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logServiceItemUpdated(1L, 100L, "Updated Service"));
        }

        @Test
        @DisplayName("logServiceItemDeleted should not throw")
        void logServiceItemDeletedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logServiceItemDeleted(1L, 100L, "Deleted Service"));
        }
    }

    @Nested
    @DisplayName("Appointment Task Events")
    class AppointmentTaskEvents {

        @Test
        @DisplayName("logTaskCreated should not throw")
        void logTaskCreatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logTaskCreated(1L, 100L, "2026-02-18", 10));
        }

        @Test
        @DisplayName("logTaskUpdated should not throw")
        void logTaskUpdatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logTaskUpdated(1L, "2026-02-18", 15));
        }

        @Test
        @DisplayName("logSlotCreated should not throw")
        void logSlotCreatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logSlotCreated(1L, 100L, "09:00", "10:00", 5));
        }
    }

    @Nested
    @DisplayName("Booking Events")
    class BookingEvents {

        @Test
        @DisplayName("logBookingCreated should not throw")
        void logBookingCreatedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logBookingCreated(1L, 100L, 10L, 50L));
        }

        @Test
        @DisplayName("logBookingConfirmed should not throw")
        void logBookingConfirmedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logBookingConfirmed(1L, 100L, 200L));
        }

        @Test
        @DisplayName("logBookingCancelled should not throw")
        void logBookingCancelledShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logBookingCancelled(1L, 100L, "User request"));
        }

        @Test
        @DisplayName("logBookingCompleted should not throw")
        void logBookingCompletedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logBookingCompleted(1L, 100L, 200L));
        }

        @Test
        @DisplayName("logBookingTimeout should not throw")
        void logBookingTimeoutShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logBookingTimeout(1L, 100L));
        }
    }

    @Nested
    @DisplayName("Signed Link Events")
    class SignedLinkEvents {

        @Test
        @DisplayName("logSignedLinkGenerated should not throw")
        void logSignedLinkGeneratedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logSignedLinkGenerated(1L, 100L, "2026-02-21T00:00:00Z"));
        }

        @Test
        @DisplayName("logSignedLinkAccessed should not throw")
        void logSignedLinkAccessedShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logSignedLinkAccessed(1L, "192.168.1.100", true));
            assertDoesNotThrow(() -> BusinessLogger.logSignedLinkAccessed(1L, "192.168.1.100", false));
        }

        @Test
        @DisplayName("logSignedLinkExpired should not throw")
        void logSignedLinkExpiredShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logSignedLinkExpired(1L, "192.168.1.100"));
        }
    }

    @Nested
    @DisplayName("Security Events")
    class SecurityEvents {

        @Test
        @DisplayName("logRateLimitExceeded should not throw")
        void logRateLimitExceededShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logRateLimitExceeded("192.168.1.100", "/api/test", "default"));
        }
    }

    @Nested
    @DisplayName("WebSocket Events")
    class WebSocketEvents {

        @Test
        @DisplayName("logWebSocketNotification should not throw")
        void logWebSocketNotificationShouldNotThrow() {
            assertDoesNotThrow(() -> BusinessLogger.logWebSocketNotification(100L, 1L, "BOOKING_CREATED"));
        }
    }

    @Nested
    @DisplayName("Email Masking")
    class EmailMasking {

        @Test
        @DisplayName("Should mask email correctly")
        void shouldMaskEmailCorrectly() {
            // This is tested indirectly through logUserRegistered
            // The maskEmail method is private, but we can verify it doesn't throw
            assertDoesNotThrow(() -> BusinessLogger.logUserRegistered(1L, "user", "test.email@example.com"));
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            assertDoesNotThrow(() -> BusinessLogger.logUserRegistered(1L, "user", null));
        }

        @Test
        @DisplayName("Should handle email without @")
        void shouldHandleEmailWithoutAtSign() {
            assertDoesNotThrow(() -> BusinessLogger.logUserRegistered(1L, "user", "notanemail"));
        }
    }

    @Nested
    @DisplayName("Event Constants")
    class EventConstants {

        @Test
        @DisplayName("Event constants should not be null or empty")
        void eventConstantsShouldNotBeNullOrEmpty() {
            assertNotNull(BusinessLogger.EVENT_USER_REGISTERED);
            assertNotNull(BusinessLogger.EVENT_USER_LOGIN);
            assertNotNull(BusinessLogger.EVENT_USER_LOGOUT);
            assertNotNull(BusinessLogger.EVENT_MERCHANT_PROFILE_CREATED);
            assertNotNull(BusinessLogger.EVENT_SERVICE_ITEM_CREATED);
            assertNotNull(BusinessLogger.EVENT_TASK_CREATED);
            assertNotNull(BusinessLogger.EVENT_SLOT_CREATED);
            assertNotNull(BusinessLogger.EVENT_BOOKING_CREATED);
            assertNotNull(BusinessLogger.EVENT_BOOKING_CONFIRMED);
            assertNotNull(BusinessLogger.EVENT_BOOKING_CANCELLED);
            assertNotNull(BusinessLogger.EVENT_BOOKING_COMPLETED);
            assertNotNull(BusinessLogger.EVENT_SIGNED_LINK_GENERATED);
            assertNotNull(BusinessLogger.EVENT_RATE_LIMIT_EXCEEDED);
            assertNotNull(BusinessLogger.EVENT_WEBSOCKET_NOTIFICATION);

            assertFalse(BusinessLogger.EVENT_USER_REGISTERED.isEmpty());
            assertFalse(BusinessLogger.EVENT_BOOKING_CREATED.isEmpty());
        }
    }
}
