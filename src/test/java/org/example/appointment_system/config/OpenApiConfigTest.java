package org.example.appointment_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenApiConfig.
 */
class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "serverPort", "8080");
    }

    @Test
    @DisplayName("Should create OpenAPI bean with correct API info")
    void shouldCreateOpenAPIWithCorrectInfo() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();

        assertNotNull(openAPI);
        Info info = openAPI.getInfo();
        assertNotNull(info);
        assertEquals("预约系统 API", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertNotNull(info.getDescription());
        assertTrue(info.getDescription().contains("预约平台"));
        assertNotNull(info.getContact());
        assertEquals("预约系统支持", info.getContact().getName());
        assertNotNull(info.getLicense());
        assertEquals("Apache 2.0", info.getLicense().getName());
    }

    @Test
    @DisplayName("Should configure servers correctly")
    void shouldConfigureServersCorrectly() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();

        List<Server> servers = openAPI.getServers();
        assertNotNull(servers);
        assertEquals(2, servers.size());

        Server devServer = servers.get(0);
        assertEquals("http://localhost:8080", devServer.getUrl());
        assertEquals("开发服务器", devServer.getDescription());

        Server prodServer = servers.get(1);
        assertEquals("https://api.appointment-system.example.com", prodServer.getUrl());
        assertEquals("生产服务器", prodServer.getDescription());
    }

    @Test
    @DisplayName("Should configure session security scheme")
    void shouldConfigureSessionSecurityScheme() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("sessionAuth"));

        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("sessionAuth");
        assertEquals(SecurityScheme.Type.APIKEY, securityScheme.getType());
        assertEquals(SecurityScheme.In.COOKIE, securityScheme.getIn());
        assertEquals("SESSION", securityScheme.getName());
        assertNotNull(securityScheme.getDescription());
        assertTrue(securityScheme.getDescription().contains("基于Cookie的会话认证"));
    }

    @Test
    @DisplayName("Should add security requirement to OpenAPI")
    void shouldAddSecurityRequirement() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();

        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
        assertTrue(openAPI.getSecurity().get(0).containsKey("sessionAuth"));
    }

    @Test
    @DisplayName("Should include authentication documentation in description")
    void shouldIncludeAuthDocumentation() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();
        String description = openAPI.getInfo().getDescription();

        assertTrue(description.contains("认证"));
        assertTrue(description.contains("POST /api/auth/login"));
        assertTrue(description.contains("POST /api/auth/logout"));
    }

    @Test
    @DisplayName("Should include rate limiting documentation in description")
    void shouldIncludeRateLimitDocumentation() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();
        String description = openAPI.getInfo().getDescription();

        assertTrue(description.contains("限流"));
        assertTrue(description.contains("60次/分钟"));
        assertTrue(description.contains("120次/分钟"));
    }

    @Test
    @DisplayName("Should include error handling documentation in description")
    void shouldIncludeErrorHandlingDocumentation() {
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();
        String description = openAPI.getInfo().getDescription();

        assertTrue(description.contains("错误处理"));
    }

    @Test
    @DisplayName("Should use custom server port")
    void shouldUseCustomServerPort() {
        ReflectionTestUtils.setField(openApiConfig, "serverPort", "9090");
        OpenAPI openAPI = openApiConfig.appointmentSystemOpenAPI();

        Server devServer = openAPI.getServers().get(0);
        assertEquals("http://localhost:9090", devServer.getUrl());
    }
}
