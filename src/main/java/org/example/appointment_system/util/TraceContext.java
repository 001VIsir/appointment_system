package org.example.appointment_system.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.example.appointment_system.filter.RequestLoggingFilter.*;

/**
 * 用于管理追踪上下文（TraceId、SpanId等）的工具类。
 *
 * 提供以下方法：
 * - 获取/设置追踪上下文值
 * - 使用特定追踪上下文运行代码
 * - 跨线程边界传播追踪上下文
 */
public final class TraceContext {

    private TraceContext() {
        // 工具类
    }

    // ==================== Getters ====================

    /**
     * 获取当前TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 获取当前SpanId
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /**
     * 获取当前RequestId
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /**
     * 获取当前UserId
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * 获取当前MerchantId
     */
    public static String getMerchantId() {
        return MDC.get(MERCHANT_ID);
    }

    /**
     * 获取当前ClientIp
     */
    public static String getClientIp() {
        return MDC.get(CLIENT_IP);
    }

    /**
     * 获取所有当前MDC值作为Map
     */
    public static Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    // ==================== Setters ====================

    /**
     * 设置TraceId
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * 设置SpanId
     */
    public static void setSpanId(String spanId) {
        if (spanId != null) {
            MDC.put(SPAN_ID, spanId);
        }
    }

    /**
     * 设置UserId
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * 设置MerchantId
     */
    public static void setMerchantId(String merchantId) {
        if (merchantId != null) {
            MDC.put(MERCHANT_ID, merchantId);
        }
    }

    /**
     * 从Map设置上下文
     */
    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    // ==================== Clear ====================

    /**
     * 清除TraceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }

    /**
     * 清除SpanId
     */
    public static void clearSpanId() {
        MDC.remove(SPAN_ID);
    }

    /**
     * 清除UserId
     */
    public static void clearUserId() {
        MDC.remove(USER_ID);
    }

    /**
     * 清除MerchantId
     */
    public static void clearMerchantId() {
        MDC.remove(MERCHANT_ID);
    }

    /**
     * 清除所有MDC上下文
     */
    public static void clear() {
        MDC.clear();
    }

    // ==================== Context Propagation ====================

    /**
     * 使用当前追踪上下文执行Runnable任务
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
     * 使用当前追踪上下文执行Callable任务
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
     * 使用指定追踪上下文执行任务
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
     * 使用指定追踪上下文执行任务并返回结果
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
     * 检查追踪上下文是否激活
     */
    public static boolean hasTraceContext() {
        String traceId = getTraceId();
        return traceId != null && !traceId.isEmpty();
    }

    /**
     * 检查用户上下文是否可用
     */
    public static boolean hasUserContext() {
        String userId = getUserId();
        return userId != null && !userId.isEmpty();
    }
}
