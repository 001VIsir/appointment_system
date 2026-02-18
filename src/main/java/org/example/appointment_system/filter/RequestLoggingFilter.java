package org.example.appointment_system.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求日志过滤器
 *
 * 功能：
 * - 生成并传播TraceId用于分布式追踪
 * - 生成SpanId用于请求跟踪
 * - 记录请求详情（方法、URI、客户端IP、用户代理）
 * - 记录响应状态和耗时
 * - 填充MDC用于结构化日志
 */
@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // MDC Keys
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String MERCHANT_ID = "merchantId";
    public static final String CLIENT_IP = "clientIp";
    public static final String REQUEST_URI = "requestUri";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String USER_AGENT = "userAgent";

    // 请求头
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";

    // 跳过日志记录的路径
    private static final String[] SKIP_PATHS = {
            "/actuator/health",
            "/actuator/prometheus",
            "/swagger-ui",
            "/api-docs",
            "/favicon.ico",
            "/error"
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("RequestLoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) ||
            !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String requestUri = httpRequest.getRequestURI();

        // 跳过某些路径的日志记录
        if (shouldSkipLogging(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 设置MDC上下文
            setupMDC(httpRequest);

            // 记录请求
            logRequest(httpRequest);

            // 继续过滤器链
            chain.doFilter(request, response);

        } finally {
            // 记录响应
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, httpResponse, duration);

            // 清除MDC
            clearMDC();
        }
    }

    @Override
    public void destroy() {
        log.info("RequestLoggingFilter destroyed");
    }

    /**
     * 使用追踪和请求信息设置MDC上下文
     */
    private void setupMDC(HttpServletRequest request) {
        // 追踪ID - 使用现有头或生成新的
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID, traceId);

        // 跨度ID - 每个请求都生成新的
        MDC.put(SPAN_ID, generateSpanId());

        // 请求ID
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));

        // 客户端IP
        MDC.put(CLIENT_IP, getClientIp(request));

        // 请求详情
        MDC.put(REQUEST_URI, request.getRequestURI());
        MDC.put(HTTP_METHOD, request.getMethod());

        // 用户代理
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        if (userAgent != null && userAgent.length() > 100) {
            userAgent = userAgent.substring(0, 100) + "...";
        }
        MDC.put(USER_AGENT, userAgent);

        // 来自安全上下文的用户信息
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(USER_ID, auth.getName());

                // 如果可用，提取商家ID（来自自定义用户详情）
                Object principal = auth.getPrincipal();
                if (principal instanceof MerchantAwareUserDetails merchantDetails) {
                    Long merchantId = merchantDetails.getMerchantId();
                    if (merchantId != null) {
                        MDC.put(MERCHANT_ID, merchantId.toString());
                    }
                }
            }
        } catch (Exception e) {
            // 忽略安全上下文访问错误
        }
    }

    /**
     * 记录传入请求
     */
    private void logRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String uri = queryString != null
                ? request.getRequestURI() + "?" + queryString
                : request.getRequestURI();

        log.info(">>> {} {} from {}",
                request.getMethod(),
                uri,
                MDC.get(CLIENT_IP));
    }

    /**
     * 记录响应
     */
    private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        int status = response.getStatus();

        String logLevel = "INFO";
        if (status >= 500) {
            logLevel = "ERROR";
        } else if (status >= 400) {
            logLevel = "WARN";
        }

        String queryString = request.getQueryString();
        String uri = queryString != null
                ? request.getRequestURI() + "?" + queryString
                : request.getRequestURI();

        switch (logLevel) {
            case "ERROR" ->
                    log.error("<<< {} {} - {} ({}ms)",
                            request.getMethod(), uri, status, duration);
            case "WARN" ->
                    log.warn("<<< {} {} - {} ({}ms)",
                            request.getMethod(), uri, status, duration);
            default ->
                    log.info("<<< {} {} - {} ({}ms)",
                            request.getMethod(), uri, status, duration);
        }
    }

    /**
     * 清除MDC上下文
     */
    private void clearMDC() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(REQUEST_ID);
        MDC.remove(USER_ID);
        MDC.remove(MERCHANT_ID);
        MDC.remove(CLIENT_IP);
        MDC.remove(REQUEST_URI);
        MDC.remove(HTTP_METHOD);
        MDC.remove(USER_AGENT);
    }

    /**
     * 检查路径是否应跳过日志记录
     */
    private boolean shouldSkipLogging(String requestUri) {
        if (requestUri == null) {
            return true;
        }
        for (String skipPath : SKIP_PATHS) {
            if (requestUri.startsWith(skipPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求中获取客户端IP，处理代理
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }

        ip = request.getHeader(HEADER_X_REAL_IP);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 生成TraceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 生成SpanId
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 从用户详情中提取商家ID的接口
     */
    public interface MerchantAwareUserDetails {
        Long getMerchantId();
    }
}
