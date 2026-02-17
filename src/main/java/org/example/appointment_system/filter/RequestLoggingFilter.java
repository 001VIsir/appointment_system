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
 * Request Logging Filter
 *
 * Features:
 * - Generates and propagates TraceId for distributed tracing
 * - Generates SpanId for request tracking
 * - Logs request details (method, URI, client IP, user agent)
 * - Logs response status and duration
 * - Populates MDC for structured logging
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

    // Request headers
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";

    // Paths to skip logging
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

        // Skip logging for certain paths
        if (shouldSkipLogging(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // Setup MDC context
            setupMDC(httpRequest);

            // Log request
            logRequest(httpRequest);

            // Continue with filter chain
            chain.doFilter(request, response);

        } finally {
            // Log response
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, httpResponse, duration);

            // Clear MDC
            clearMDC();
        }
    }

    @Override
    public void destroy() {
        log.info("RequestLoggingFilter destroyed");
    }

    /**
     * Setup MDC context with tracing and request information
     */
    private void setupMDC(HttpServletRequest request) {
        // TraceId - use existing from header or generate new one
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID, traceId);

        // SpanId - always generate new for each request
        MDC.put(SPAN_ID, generateSpanId());

        // RequestId
        MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, 8));

        // Client IP
        MDC.put(CLIENT_IP, getClientIp(request));

        // Request details
        MDC.put(REQUEST_URI, request.getRequestURI());
        MDC.put(HTTP_METHOD, request.getMethod());

        // User agent
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        if (userAgent != null && userAgent.length() > 100) {
            userAgent = userAgent.substring(0, 100) + "...";
        }
        MDC.put(USER_AGENT, userAgent);

        // User information from security context
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(USER_ID, auth.getName());

                // Extract merchant ID if available (from custom user details)
                Object principal = auth.getPrincipal();
                if (principal instanceof MerchantAwareUserDetails merchantDetails) {
                    Long merchantId = merchantDetails.getMerchantId();
                    if (merchantId != null) {
                        MDC.put(MERCHANT_ID, merchantId.toString());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore security context access errors
        }
    }

    /**
     * Log incoming request
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
     * Log outgoing response
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
     * Clear MDC context
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
     * Check if path should skip logging
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
     * Get client IP from request, handling proxies
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For may contain multiple IPs, take the first one
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
     * Generate TraceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Generate SpanId
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * Interface for extracting merchant ID from user details
     */
    public interface MerchantAwareUserDetails {
        Long getMerchantId();
    }
}
