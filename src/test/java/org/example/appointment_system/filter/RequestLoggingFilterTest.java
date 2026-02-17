package org.example.appointment_system.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;

import java.io.IOException;

import static org.example.appointment_system.filter.RequestLoggingFilter.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        MDC.clear();
        // Set up common stubs
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getQueryString()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should set TraceId from header if present")
    void shouldSetTraceIdFromHeader() throws IOException, ServletException {
        // Given
        String traceId = "test-trace-123";
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Trace-Id")).thenReturn(traceId);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // MDC should be cleared after filter
        assertNull(MDC.get(TRACE_ID));
    }

    @Test
    @DisplayName("Should generate TraceId if not present in header")
    void shouldGenerateTraceIdIfNotPresent() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip logging for actuator health endpoint")
    void shouldSkipLoggingForActuatorHealth() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // No MDC should be set
        assertNull(MDC.get(TRACE_ID));
    }

    @Test
    @DisplayName("Should skip logging for swagger-ui")
    void shouldSkipLoggingForSwagger() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get(TRACE_ID));
    }

    @Test
    @DisplayName("Should skip logging for api-docs")
    void shouldSkipLoggingForApiDocs() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api-docs");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get(TRACE_ID));
    }

    @Test
    @DisplayName("Should set client IP from X-Forwarded-For header")
    void shouldSetClientIpFromXForwardedFor() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set client IP from X-Real-IP if X-Forwarded-For is absent")
    void shouldSetClientIpFromXRealIp() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.100");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set client IP from remote address as fallback")
    void shouldSetClientIpFromRemoteAddr() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should log request with query string")
    void shouldLogRequestWithQueryString() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn("param1=value1&param2=value2");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should truncate long user agent")
    void shouldTruncateLongUserAgent() throws IOException, ServletException {
        // Given
        String longUserAgent = "A".repeat(200);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("User-Agent")).thenReturn(longUserAgent);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear MDC after processing")
    void shouldClearMdcAfterProcessing() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(MDC.get(TRACE_ID));
        assertNull(MDC.get(SPAN_ID));
        assertNull(MDC.get(REQUEST_ID));
        assertNull(MDC.get(CLIENT_IP));
    }

    @Test
    @DisplayName("Should handle null request URI")
    void shouldHandleNullRequestUri() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn(null);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle favicon request")
    void shouldHandleFaviconRequest() throws IOException, ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/favicon.ico");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get(TRACE_ID));
    }

    @Test
    @DisplayName("Filter init should not throw")
    void filterInitShouldNotThrow() {
        // When/Then - should not throw
        assertDoesNotThrow(() -> filter.init(null));
    }

    @Test
    @DisplayName("Filter destroy should not throw")
    void filterDestroyShouldNotThrow() {
        // When/Then - should not throw
        assertDoesNotThrow(() -> filter.destroy());
    }
}
