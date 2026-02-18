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
 * HTTP请求限流过滤器。
 *
 * <p>此过滤器使用Redis支持的滑动窗口算法实现限流。根据以下条件应用不同的限流：</p>
 * <ul>
 *   <li>已认证用户：基于用户ID的更高限制</li>
 *   <li>匿名用户：基于IP地址的更低限制</li>
 *   <li>特定接口：对敏感操作的自定义限制</li>
 * </ul>
 *
 * <h3>限流规则：</h3>
 * <ul>
 *   <li>匿名（基于IP）：60次/分钟</li>
 *   <li>已认证（基于用户）：120次/分钟</li>
 *   <li>认证接口（/api/auth/*）：10次/分钟（更严格的安全限制）</li>
 * </ul>
 *
 * <h3>响应头：</h3>
 * <ul>
 *   <li>X-RateLimit-Limit：每个窗口期的最大请求数</li>
 *   <li>X-RateLimit-Remaining：当前窗口期剩余请求数</li>
 *   <li>X-RateLimit-Reset：窗口重置时的Unix时间戳</li>
 * </ul>
 *
 * <h3>错误响应：</h3>
 * <p>超限时返回HTTP 429（请求过多）及JSON响应体：</p>
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

    // 特定接口模式的自定义限流
    private static final int AUTH_ENDPOINT_LIMIT = 10;  // 认证接口更严格
    private static final int PUBLIC_ENDPOINT_LIMIT = 30; // 公开接口更严格

    // 带自定义限制的接口模式
    private static final String AUTH_ENDPOINT_PREFIX = "/api/auth/";
    private static final String PUBLIC_ENDPOINT_PREFIX = "/api/public/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 如果禁用则跳过限流
        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 跳过健康检查和actuator的限流
        String path = httpRequest.getRequestURI();
        if (shouldSkipRateLimiting(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取客户端标识符并检查限流
        String clientId = getClientIdentifier(httpRequest);
        boolean isAuthenticated = isAuthenticated();
        RateLimitResult result = checkRateLimit(path, clientId, isAuthenticated);

        // 添加限流头信息
        addRateLimitHeaders(httpResponse, result);

        if (!result.allowed()) {
            handleRateLimitExceeded(httpResponse, result);
            return;
        }

        // 接近限制时记录日志
        if (result.isApproachingLimit(20)) {
            log.warn("Client {} approaching rate limit: {}/{} requests",
                clientId, result.currentCount(), result.limit());
        }

        chain.doFilter(request, response);
    }

    /**
     * 检查路径是否应跳过限流。
     *
     * @param path 请求路径
     * @return 如果应跳过限流返回true
     */
    private boolean shouldSkipRateLimiting(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/error");
    }

    /**
     * 获取限流的客户端标识符。
     *
     * <p>对于已认证用户，使用用户ID。对于匿名用户，
     * 使用IP地址（支持X-Forwarded-For头）。</p>
     *
     * @param request HTTP请求
     * @return 客户端标识符
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // 首先检查已认证用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return "user:" + userDetails.getId();
        }

        // 回退到IP地址
        String ipAddress = getClientIpAddress(request);
        return "ip:" + ipAddress;
    }

    /**
     * 获取客户端IP地址，考虑代理头。
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 取链中的第一个IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 检查当前用户是否已认证。
     *
     * @return 已认证返回true
     */
    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               !(authentication.getPrincipal() instanceof String);
    }

    /**
     * 根据接口和客户端检查限流。
     *
     * @param path 请求路径
     * @param clientId 客户端标识符
     * @param isAuthenticated 客户端是否已认证
     * @return 限流结果
     */
    private RateLimitResult checkRateLimit(String path, String clientId, boolean isAuthenticated) {
        // 对认证接口应用更严格的限制
        if (path.startsWith(AUTH_ENDPOINT_PREFIX)) {
            return rateLimitService.checkRateLimit(clientId, isAuthenticated, AUTH_ENDPOINT_LIMIT);
        }

        // 对公开接口应用更严格的限制
        if (path.startsWith(PUBLIC_ENDPOINT_PREFIX)) {
            return rateLimitService.checkRateLimit(clientId, isAuthenticated, PUBLIC_ENDPOINT_LIMIT);
        }

        // 默认限流
        return rateLimitService.checkRateLimit(clientId, isAuthenticated);
    }

    /**
     * 向响应添加限流头信息。
     *
     * @param response HTTP响应
     * @param result 限流结果
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetTimeMs() / 1000));
    }

    /**
     * 处理超出限流的情况，返回429响应。
     *
     * @param response HTTP响应
     * @param result 限流结果
     * @throws IOException 写入响应失败
     */
    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitResult result) throws IOException {
        response.setStatus(429); // HTTP 429 请求过多
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 计算重试等待时间（秒）
        long retryAfter = Math.max(1, (result.resetTimeMs() - System.currentTimeMillis()) / 1000);
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        // 构建错误响应
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
