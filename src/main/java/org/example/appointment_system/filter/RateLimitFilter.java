package org.example.appointment_system.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.service.RateLimitService;
import org.example.appointment_system.service.RateLimitService.RateLimitResult;
import org.example.appointment_system.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter for rate limiting HTTP requests.
 *
 * <p>This filter implements rate limiting using a sliding window algorithm
 * backed by Redis. It applies different rate limits based on:</p>
 * <ul>
 *   <li>Authenticated users: Higher limits based on user ID</li>
 *   <li>Anonymous users: Lower limits based on IP address</li>
 *   <li>Specific endpoints: Custom limits for sensitive operations</li>
 * </ul>
 *
 * <h3>Rate Limits:</h3>
 * <ul>
 *   <li>Anonymous (IP-based): 60 requests/minute</li>
 *   <li>Authenticated (User-based): 120 requests/minute</li>
 *   <li>Auth endpoints (/api/auth/*): 10 requests/minute (stricter for security)</li>
 * </ul>
 *
 * <h3>Response Headers:</h3>
 * <ul>
 *   <li>X-RateLimit-Limit: Maximum requests per window</li>
 *   <li>X-RateLimit-Remaining: Remaining requests in current window</li>
 *   <li>X-RateLimit-Reset: Unix timestamp when the window resets</li>
 * </ul>
 *
 * <h3>Error Response:</h3>
 * <p>When rate limited, returns HTTP 429 (Too Many Requests) with JSON body:</p>
 * <pre>
 * {
 *   "error": "Too Many Requests",
 *   "message": "Rate limit exceeded. Please try again later.",
 *   "retryAfter": 60
 * }
 * </pre>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.default-per-minute:60}")
    private int defaultLimit;

    @Value("${app.rate-limit.authenticated-per-minute:120}")
    private int authenticatedLimit;

    @Value("${app.rate-limit.auth-endpoint-per-minute:10}")
    private int authEndpointLimit;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    // Custom rate limits for specific endpoint patterns
    private static final int AUTH_ENDPOINT_LIMIT = 10;  // Stricter for auth endpoints
    private static final int PUBLIC_ENDPOINT_LIMIT = 30; // Stricter for public endpoints

    // Endpoint patterns with custom limits
    private static final String AUTH_ENDPOINT_PREFIX = "/api/auth/";
    private static final String PUBLIC_ENDPOINT_PREFIX = "/api/public/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Skip rate limiting if disabled
        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for health checks and actuator
        String path = httpRequest.getRequestURI();
        if (shouldSkipRateLimiting(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Get client identifier and check rate limit
        String clientId = getClientIdentifier(httpRequest);
        boolean isAuthenticated = isAuthenticated();
        RateLimitResult result = checkRateLimit(path, clientId, isAuthenticated);

        // Add rate limit headers
        addRateLimitHeaders(httpResponse, result);

        if (!result.allowed()) {
            handleRateLimitExceeded(httpResponse, result);
            return;
        }

        // Log if approaching limit
        if (result.isApproachingLimit(20)) {
            log.warn("Client {} approaching rate limit: {}/{} requests",
                clientId, result.currentCount(), result.limit());
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if the path should skip rate limiting.
     *
     * @param path the request path
     * @return true if rate limiting should be skipped
     */
    private boolean shouldSkipRateLimiting(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/error");
    }

    /**
     * Get the client identifier for rate limiting.
     *
     * <p>For authenticated users, uses user ID. For anonymous users,
     * uses IP address (with X-Forwarded-For header support).</p>
     *
     * @param request the HTTP request
     * @return the client identifier
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Check for authenticated user first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return "user:" + userDetails.getId();
        }

        // Fall back to IP address
        String ipAddress = getClientIpAddress(request);
        return "ip:" + ipAddress;
    }

    /**
     * Get the client IP address, considering proxy headers.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Check if the current user is authenticated.
     *
     * @return true if authenticated
     */
    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               !(authentication.getPrincipal() instanceof String);
    }

    /**
     * Check rate limit based on endpoint and client.
     *
     * @param path the request path
     * @param clientId the client identifier
     * @param isAuthenticated whether the client is authenticated
     * @return the rate limit result
     */
    private RateLimitResult checkRateLimit(String path, String clientId, boolean isAuthenticated) {
        // Apply stricter limits for auth endpoints
        if (path.startsWith(AUTH_ENDPOINT_PREFIX)) {
            return rateLimitService.checkRateLimit(clientId, isAuthenticated, AUTH_ENDPOINT_LIMIT);
        }

        // Apply stricter limits for public endpoints
        if (path.startsWith(PUBLIC_ENDPOINT_PREFIX)) {
            return rateLimitService.checkRateLimit(clientId, isAuthenticated, PUBLIC_ENDPOINT_LIMIT);
        }

        // Default rate limiting
        return rateLimitService.checkRateLimit(clientId, isAuthenticated);
    }

    /**
     * Add rate limit headers to the response.
     *
     * @param response the HTTP response
     * @param result the rate limit result
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetTimeMs() / 1000));
    }

    /**
     * Handle rate limit exceeded by returning 429 response.
     *
     * @param response the HTTP response
     * @param result the rate limit result
     * @throws IOException if writing response fails
     */
    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitResult result) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Calculate retry-after in seconds
        long retryAfter = Math.max(1, (result.resetTimeMs() - System.currentTimeMillis()) / 1000);
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        // Build error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Rate limit exceeded. Please try again later.");
        errorResponse.put("retryAfter", retryAfter);
        errorResponse.put("limit", result.limit());

        log.warn("Rate limit exceeded: limit={}, currentCount={}, retryAfter={}s",
            result.limit(), result.currentCount(), retryAfter);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
