package org.example.appointment_system.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.appointment_system.filter.RequestLoggingFilter.*;
import static org.junit.jupiter.api.Assertions.*;

class TraceContextTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("getTraceId should return value from MDC")
        void getTraceIdShouldReturnValueFromMdc() {
            MDC.put(TRACE_ID, "test-trace-123");
            assertEquals("test-trace-123", TraceContext.getTraceId());
        }

        @Test
        @DisplayName("getTraceId should return null if not set")
        void getTraceIdShouldReturnNullIfNotSet() {
            assertNull(TraceContext.getTraceId());
        }

        @Test
        @DisplayName("getSpanId should return value from MDC")
        void getSpanIdShouldReturnValueFromMdc() {
            MDC.put(SPAN_ID, "span-123");
            assertEquals("span-123", TraceContext.getSpanId());
        }

        @Test
        @DisplayName("getRequestId should return value from MDC")
        void getRequestIdShouldReturnValueFromMdc() {
            MDC.put(REQUEST_ID, "req-123");
            assertEquals("req-123", TraceContext.getRequestId());
        }

        @Test
        @DisplayName("getUserId should return value from MDC")
        void getUserIdShouldReturnValueFromMdc() {
            MDC.put(USER_ID, "user-123");
            assertEquals("user-123", TraceContext.getUserId());
        }

        @Test
        @DisplayName("getMerchantId should return value from MDC")
        void getMerchantIdShouldReturnValueFromMdc() {
            MDC.put(MERCHANT_ID, "merchant-123");
            assertEquals("merchant-123", TraceContext.getMerchantId());
        }

        @Test
        @DisplayName("getClientIp should return value from MDC")
        void getClientIpShouldReturnValueFromMdc() {
            MDC.put(CLIENT_IP, "192.168.1.100");
            assertEquals("192.168.1.100", TraceContext.getClientIp());
        }

        @Test
        @DisplayName("getCopyOfContextMap should return copy of MDC")
        void getCopyOfContextMapShouldReturnCopyOfMdc() {
            MDC.put(TRACE_ID, "trace-123");
            MDC.put(USER_ID, "user-456");

            Map<String, String> contextMap = TraceContext.getCopyOfContextMap();

            assertNotNull(contextMap);
            assertEquals("trace-123", contextMap.get(TRACE_ID));
            assertEquals("user-456", contextMap.get(USER_ID));

            // Verify it's a copy - changes to original should not affect it
            MDC.put(TRACE_ID, "new-trace");
            assertEquals("trace-123", contextMap.get(TRACE_ID));
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("setTraceId should set value in MDC")
        void setTraceIdShouldSetValueInMdc() {
            TraceContext.setTraceId("new-trace-123");
            assertEquals("new-trace-123", MDC.get(TRACE_ID));
        }

        @Test
        @DisplayName("setTraceId should handle null value")
        void setTraceIdShouldHandleNullValue() {
            MDC.put(TRACE_ID, "existing-trace");
            TraceContext.setTraceId(null);
            assertEquals("existing-trace", MDC.get(TRACE_ID)); // Should not change
        }

        @Test
        @DisplayName("setSpanId should set value in MDC")
        void setSpanIdShouldSetValueInMdc() {
            TraceContext.setSpanId("span-456");
            assertEquals("span-456", MDC.get(SPAN_ID));
        }

        @Test
        @DisplayName("setUserId should set value in MDC")
        void setUserIdShouldSetValueInMdc() {
            TraceContext.setUserId("user-789");
            assertEquals("user-789", MDC.get(USER_ID));
        }

        @Test
        @DisplayName("setMerchantId should set value in MDC")
        void setMerchantIdShouldSetValueInMdc() {
            TraceContext.setMerchantId("merchant-999");
            assertEquals("merchant-999", MDC.get(MERCHANT_ID));
        }

        @Test
        @DisplayName("setContextMap should set all values")
        void setContextMapShouldSetAllValues() {
            Map<String, String> contextMap = new HashMap<>();
            contextMap.put(TRACE_ID, "trace-abc");
            contextMap.put(USER_ID, "user-xyz");

            TraceContext.setContextMap(contextMap);

            assertEquals("trace-abc", MDC.get(TRACE_ID));
            assertEquals("user-xyz", MDC.get(USER_ID));
        }

        @Test
        @DisplayName("setContextMap should handle null")
        void setContextMapShouldHandleNull() {
            assertDoesNotThrow(() -> TraceContext.setContextMap(null));
        }
    }

    @Nested
    @DisplayName("Clear")
    class Clear {

        @Test
        @DisplayName("clearTraceId should remove traceId from MDC")
        void clearTraceIdShouldRemoveTraceIdFromMdc() {
            MDC.put(TRACE_ID, "trace-to-clear");
            TraceContext.clearTraceId();
            assertNull(MDC.get(TRACE_ID));
        }

        @Test
        @DisplayName("clearSpanId should remove spanId from MDC")
        void clearSpanIdShouldRemoveSpanIdFromMdc() {
            MDC.put(SPAN_ID, "span-to-clear");
            TraceContext.clearSpanId();
            assertNull(MDC.get(SPAN_ID));
        }

        @Test
        @DisplayName("clearUserId should remove userId from MDC")
        void clearUserIdShouldRemoveUserIdFromMdc() {
            MDC.put(USER_ID, "user-to-clear");
            TraceContext.clearUserId();
            assertNull(MDC.get(USER_ID));
        }

        @Test
        @DisplayName("clearMerchantId should remove merchantId from MDC")
        void clearMerchantIdShouldRemoveMerchantIdFromMdc() {
            MDC.put(MERCHANT_ID, "merchant-to-clear");
            TraceContext.clearMerchantId();
            assertNull(MDC.get(MERCHANT_ID));
        }

        @Test
        @DisplayName("clear should remove all values from MDC")
        void clearShouldRemoveAllValuesFromMdc() {
            MDC.put(TRACE_ID, "trace");
            MDC.put(SPAN_ID, "span");
            MDC.put(USER_ID, "user");
            MDC.put(MERCHANT_ID, "merchant");

            TraceContext.clear();

            assertNull(MDC.get(TRACE_ID));
            assertNull(MDC.get(SPAN_ID));
            assertNull(MDC.get(USER_ID));
            assertNull(MDC.get(MERCHANT_ID));
        }
    }

    @Nested
    @DisplayName("Context Propagation")
    class ContextPropagation {

        @Test
        @DisplayName("wrap(Runnable) should propagate context")
        void wrapRunnableShouldPropagateContext() {
            MDC.put(TRACE_ID, "original-trace");

            AtomicReference<String> capturedTraceId = new AtomicReference<>();
            Runnable wrapped = TraceContext.wrap(() -> capturedTraceId.set(MDC.get(TRACE_ID)));

            // Clear before running wrapped
            MDC.clear();

            wrapped.run();

            assertEquals("original-trace", capturedTraceId.get());
        }

        @Test
        @DisplayName("wrap(Callable) should propagate context")
        void wrapCallableShouldPropagateContext() throws Exception {
            MDC.put(TRACE_ID, "callable-trace");

            Callable<String> wrapped = TraceContext.wrap(() -> MDC.get(TRACE_ID));

            MDC.clear();

            String result = wrapped.call();

            assertEquals("callable-trace", result);
        }

        @Test
        @DisplayName("wrap should restore previous context after execution")
        void wrapShouldRestorePreviousContext() {
            MDC.put(TRACE_ID, "previous-context");

            AtomicReference<String> afterRun = new AtomicReference<>();
            Runnable wrapped = TraceContext.wrap(() -> {
                // Do nothing
            });

            wrapped.run();

            afterRun.set(MDC.get(TRACE_ID));
            assertEquals("previous-context", afterRun.get());
        }

        @Test
        @DisplayName("runWithContext should use provided context")
        void runWithContextShouldUseProvidedContext() {
            Map<String, String> context = new HashMap<>();
            context.put(TRACE_ID, "custom-trace");

            AtomicReference<String> capturedTraceId = new AtomicReference<>();
            TraceContext.runWithContext(context, () -> capturedTraceId.set(MDC.get(TRACE_ID)));

            assertEquals("custom-trace", capturedTraceId.get());
        }

        @Test
        @DisplayName("callWithContext should use provided context and return result")
        void callWithContextShouldUseProvidedContextAndReturnResult() throws Exception {
            Map<String, String> context = new HashMap<>();
            context.put(TRACE_ID, "result-trace");

            String result = TraceContext.callWithContext(context, () -> MDC.get(TRACE_ID));

            assertEquals("result-trace", result);
        }

        @Test
        @DisplayName("runWithContext should restore previous context")
        void runWithContextShouldRestorePreviousContext() {
            MDC.put(TRACE_ID, "original");

            Map<String, String> context = new HashMap<>();
            context.put(TRACE_ID, "temporary");

            TraceContext.runWithContext(context, () -> {
                // Do nothing
            });

            assertEquals("original", MDC.get(TRACE_ID));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("hasTraceContext should return true when traceId is set")
        void hasTraceContextShouldReturnTrueWhenTraceIdIsSet() {
            MDC.put(TRACE_ID, "trace-123");
            assertTrue(TraceContext.hasTraceContext());
        }

        @Test
        @DisplayName("hasTraceContext should return false when traceId is not set")
        void hasTraceContextShouldReturnFalseWhenTraceIdIsNotSet() {
            assertFalse(TraceContext.hasTraceContext());
        }

        @Test
        @DisplayName("hasTraceContext should return false when traceId is empty")
        void hasTraceContextShouldReturnFalseWhenTraceIdIsEmpty() {
            MDC.put(TRACE_ID, "");
            assertFalse(TraceContext.hasTraceContext());
        }

        @Test
        @DisplayName("hasUserContext should return true when userId is set")
        void hasUserContextShouldReturnTrueWhenUserIdIsSet() {
            MDC.put(USER_ID, "user-123");
            assertTrue(TraceContext.hasUserContext());
        }

        @Test
        @DisplayName("hasUserContext should return false when userId is not set")
        void hasUserContextShouldReturnFalseWhenUserIdIsNotSet() {
            assertFalse(TraceContext.hasUserContext());
        }
    }
}
