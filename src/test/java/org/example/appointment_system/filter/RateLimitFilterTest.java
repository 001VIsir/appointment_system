package org.example.appointment_system.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.appointment_system.security.CustomUserDetails;
import org.example.appointment_system.service.RateLimitService;
import org.example.appointment_system.service.RateLimitService.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RateLimitFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        // 设置 @Value 字段
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(filter, "defaultLimit", 60);
        ReflectionTestUtils.setField(filter, "authenticatedLimit", 120);
        ReflectionTestUtils.setField(filter, "authEndpointLimit", 10);

        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        lenient().when(response.getWriter()).thenReturn(printWriter);
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("doFilter - should skip rate limiting for actuator endpoints")
    void doFilter_shouldSkipForActuator() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(rateLimitService, never()).checkRateLimit(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("doFilter - should skip rate limiting for swagger endpoints")
    void doFilter_shouldSkipForSwagger() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(rateLimitService, never()).checkRateLimit(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("doFilter - should allow request under limit")
    void doFilter_shouldAllowUnderLimit() throws IOException, ServletException {
        // Given
        RateLimitResult result = new RateLimitResult(true, 60, 59, System.currentTimeMillis() + 60000, 1);
        when(rateLimitService.checkRateLimit(anyString(), anyBoolean())).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("doFilter - should block request over limit")
    void doFilter_shouldBlockOverLimit() throws IOException, ServletException {
        // Given
        RateLimitResult result = new RateLimitResult(false, 60, 0, System.currentTimeMillis() + 60000, 61);
        when(rateLimitService.checkRateLimit(anyString(), anyBoolean())).thenReturn(result);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"Too Many Requests\"}");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429); // HTTP 429 Too Many Requests
        verify(response).setHeader(eq("Retry-After"), anyString());
    }

    @Test
    @DisplayName("doFilter - should apply stricter limit for auth endpoints")
    void doFilter_shouldApplyStricterAuthLimit() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        RateLimitResult result = new RateLimitResult(true, 10, 9, System.currentTimeMillis() + 60000, 1);
        when(rateLimitService.checkRateLimit(anyString(), anyBoolean(), eq(10))).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimitService).checkRateLimit(anyString(), anyBoolean(), eq(10));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter - should apply stricter limit for public endpoints")
    void doFilter_shouldApplyStricterPublicLimit() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/public/tasks/1");
        RateLimitResult result = new RateLimitResult(true, 30, 29, System.currentTimeMillis() + 60000, 1);
        when(rateLimitService.checkRateLimit(anyString(), anyBoolean(), eq(30))).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimitService).checkRateLimit(anyString(), anyBoolean(), eq(30));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter - should use IP address for anonymous users")
    void doFilter_shouldUseIpForAnonymous() throws IOException, ServletException {
        // Given
        SecurityContextHolder.clearContext();
        RateLimitResult result = new RateLimitResult(true, 60, 59, System.currentTimeMillis() + 60000, 1);
        when(rateLimitService.checkRateLimit(contains("ip:"), eq(false))).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimitService).checkRateLimit(contains("ip:"), eq(false));
    }

    @Test
    @DisplayName("doFilter - should use user ID for authenticated users")
    void doFilter_shouldUseUserIdForAuthenticated() throws IOException, ServletException {
        // Given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            RateLimitResult result = new RateLimitResult(true, 120, 119, System.currentTimeMillis() + 60000, 1);
            when(rateLimitService.checkRateLimit(contains("user:"), eq(true))).thenReturn(result);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(rateLimitService).checkRateLimit(contains("user:"), eq(true));
        }
    }

    @Test
    @DisplayName("doFilter - should handle X-Forwarded-For header")
    void doFilter_shouldHandleXForwardedFor() throws IOException, ServletException {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        SecurityContextHolder.clearContext();

        RateLimitResult result = new RateLimitResult(true, 60, 59, System.currentTimeMillis() + 60000, 1);
        when(rateLimitService.checkRateLimit(contains("192.168.1.1"), eq(false))).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimitService).checkRateLimit(contains("192.168.1.1"), eq(false));
    }

    @Test
    @DisplayName("doFilter - should handle X-Real-IP header")
    void doFilter_shouldHandleXRealIp() throws IOException, ServletException {
        // Given
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.2");
        SecurityContextHolder.clearContext();

        RateLimitResult result = new RateLimitResult(true, 60, 59, System.currentTimeMillis() + 60000, 1);
        when(rateLimitService.checkRateLimit(contains("192.168.1.2"), eq(false))).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimitService).checkRateLimit(contains("192.168.1.2"), eq(false));
    }

    @Test
    @DisplayName("doFilter - should add rate limit headers")
    void doFilter_shouldAddRateLimitHeaders() throws IOException, ServletException {
        // Given
        long resetTime = System.currentTimeMillis() + 60000;
        RateLimitResult result = new RateLimitResult(true, 60, 59, resetTime, 1);
        when(rateLimitService.checkRateLimit(anyString(), anyBoolean())).thenReturn(result);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Limit", "60");
        verify(response).setHeader("X-RateLimit-Remaining", "59");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("doFilter - should skip for error endpoint")
    void doFilter_shouldSkipForError() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/error");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(rateLimitService, never()).checkRateLimit(anyString(), anyBoolean());
    }
}
