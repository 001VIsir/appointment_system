package org.example.appointment_system.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.example.appointment_system.filter.RequestLoggingFilter.*;

/**
 * Utility class for managing trace context (TraceId, SpanId, etc.)
 *
 * Provides methods to:
 * - Get/set trace context values
 * - Run code with specific trace context
 * - Propagate trace context across thread boundaries
 */
public final class TraceContext {

    private TraceContext() {
        // Utility class
    }

    // ==================== Getters ====================

    /**
     * Get current TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Get current SpanId
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /**
     * Get current RequestId
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /**
     * Get current UserId
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * Get current MerchantId
     */
    public static String getMerchantId() {
        return MDC.get(MERCHANT_ID);
    }

    /**
     * Get current ClientIp
     */
    public static String getClientIp() {
        return MDC.get(CLIENT_IP);
    }

    /**
     * Get all current MDC values as a map
     */
    public static Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    // ==================== Setters ====================

    /**
     * Set TraceId
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * Set SpanId
     */
    public static void setSpanId(String spanId) {
        if (spanId != null) {
            MDC.put(SPAN_ID, spanId);
        }
    }

    /**
     * Set UserId
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Set MerchantId
     */
    public static void setMerchantId(String merchantId) {
        if (merchantId != null) {
            MDC.put(MERCHANT_ID, merchantId);
        }
    }

    /**
     * Set context from a map
     */
    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    // ==================== Clear ====================

    /**
     * Clear TraceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }

    /**
     * Clear SpanId
     */
    public static void clearSpanId() {
        MDC.remove(SPAN_ID);
    }

    /**
     * Clear UserId
     */
    public static void clearUserId() {
        MDC.remove(USER_ID);
    }

    /**
     * Clear MerchantId
     */
    public static void clearMerchantId() {
        MDC.remove(MERCHANT_ID);
    }

    /**
     * Clear all MDC context
     */
    public static void clear() {
        MDC.clear();
    }

    // ==================== Context Propagation ====================

    /**
     * Run a Runnable with the current trace context propagated
     */
    public static Runnable wrap(Runnable task) {
        Map<String, String> contextMap = getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                task.run();
            } finally {
                if (previousContext != null) {
                    MDC.setContextMap(previousContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * Run a Callable with the current trace context propagated
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        Map<String, String> contextMap = getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                return task.call();
            } finally {
                if (previousContext != null) {
                    MDC.setContextMap(previousContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * Run a task with a specific trace context
     */
    public static void runWithContext(Map<String, String> contextMap, Runnable task) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            task.run();
        } finally {
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            } else {
                MDC.clear();
            }
        }
    }

    /**
     * Run a task with a specific trace context and return result
     */
    public static <T> T callWithContext(Map<String, String> contextMap, Callable<T> task) throws Exception {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            return task.call();
        } finally {
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            } else {
                MDC.clear();
            }
        }
    }

    // ==================== Validation ====================

    /**
     * Check if a trace context is active
     */
    public static boolean hasTraceContext() {
        String traceId = getTraceId();
        return traceId != null && !traceId.isEmpty();
    }

    /**
     * Check if user context is available
     */
    public static boolean hasUserContext() {
        String userId = getUserId();
        return userId != null && !userId.isEmpty();
    }
}
