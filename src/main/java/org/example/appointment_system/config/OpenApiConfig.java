package org.example.appointment_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for the Appointment System API.
 *
 * <p>Configures:</p>
 * <ul>
 *   <li>API metadata (title, description, version, contact, license)</li>
 *   <li>Session-based authentication security scheme</li>
 *   <li>Server configuration</li>
 * </ul>
 *
 * <h3>Access Points:</h3>
 * <ul>
 *   <li>Swagger UI: /swagger-ui.html</li>
 *   <li>OpenAPI JSON: /api-docs</li>
 *   <li>OpenAPI YAML: /api-docs.yaml</li>
 * </ul>
 *
 * <h3>API Groups:</h3>
 * <p>APIs are organized by functional area using @Tag annotations:</p>
 * <ul>
 *   <li>Authentication - User registration, login, logout</li>
 *   <li>Merchant - Merchant profile and settings</li>
 *   <li>Service Item - Service catalog management</li>
 *   <li>Appointment Task - Appointment task and slot management</li>
 *   <li>Booking - Booking creation and management</li>
 *   <li>Signed Links - Secure link generation</li>
 *   <li>Admin - System statistics and administration</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Configure the OpenAPI specification for the Appointment System.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI appointmentSystemOpenAPI() {
        final String securitySchemeName = "sessionAuth";

        return new OpenAPI()
            // API Info
            .info(apiInfo())
            // Servers
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Development server"),
                new Server()
                    .url("https://api.appointment-system.example.com")
                    .description("Production server")
            ))
            // Security Configuration (Session-based)
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, sessionSecurityScheme())
            );
    }

    /**
     * Create API info metadata.
     *
     * @return API info instance
     */
    private Info apiInfo() {
        return new Info()
            .title("Appointment System API")
            .description("""
                ## ToC Appointment Platform API

                A comprehensive appointment booking platform where merchants can create
                appointment tasks and users can book appointments via secure signed links.

                ### Features
                - **User Management**: Registration, authentication, role-based access control
                - **Merchant Portal**: Profile management, service catalog, booking management
                - **Appointment System**: Task creation, time slot management, booking with optimistic locking
                - **Secure Links**: HMAC-signed links for public booking access
                - **Real-time Notifications**: WebSocket-based notifications for booking events
                - **Statistics & Analytics**: Booking metrics, user statistics, system monitoring

                ### Authentication
                This API uses session-based authentication. To authenticate:
                1. Call `POST /api/auth/login` with your credentials
                2. The session cookie will be automatically included in subsequent requests
                3. Call `POST /api/auth/logout` to end your session

                ### Rate Limiting
                - Anonymous users: 60 requests/minute
                - Authenticated users: 120 requests/minute
                - Authentication endpoints: 10 requests/minute

                ### Error Handling
                All errors follow a consistent format with error codes and descriptive messages.
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("Appointment System Support")
                .email("support@appointment-system.example.com")
                .url("https://appointment-system.example.com/support"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * Create session-based security scheme.
     *
     * <p>Since the application uses Spring Session with Redis, authentication
     * is handled via session cookies (JSESSIONID). This security scheme
     * documents that requirement for API consumers.</p>
     *
     * @return security scheme for session authentication
     */
    private SecurityScheme sessionSecurityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .name("SESSION")
            .description("""
                Session-based authentication using cookies.

                To authenticate:
                1. Make a POST request to /api/auth/login with username and password
                2. The server will set a SESSION cookie
                3. Include this cookie in subsequent requests

                Session timeout: 4 hours (configurable per merchant)
                """);
    }
}
