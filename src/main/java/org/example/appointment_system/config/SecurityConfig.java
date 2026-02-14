package org.example.appointment_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration.
 *
 * <p>This configuration sets up the basic security infrastructure for the appointment system,
 * including session-based authentication, CORS policies, and path-based authorization rules.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Session-based authentication stored in Redis</li>
 *   <li>CORS configuration for frontend integration</li>
 *   <li>CSRF protection disabled for stateless API access</li>
 *   <li>Path-based authorization rules</li>
 *   <li>BCrypt password encoding</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Configure the security filter chain.
     *
     * <p>Defines the security rules for HTTP requests including:</p>
     * <ul>
     *   <li>Public endpoints: health checks, OpenAPI docs, public booking pages</li>
     *   <li>Protected endpoints: require authentication</li>
     *   <li>Admin endpoints: require ADMIN role</li>
     * </ul>
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF for API usage (using session-based auth with same-site cookies)
            .csrf(AbstractHttpConfigurer::disable)

            // Configure session management - use Redis-backed sessions
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/public/**",
                    "/error"
                ).permitAll()

                // Admin endpoints - require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Merchant endpoints - require MERCHANT role
                .requestMatchers("/api/merchants/**").hasAnyRole("MERCHANT", "ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Configure form login (disabled for API-only usage)
            .formLogin(AbstractHttpConfigurer::disable)

            // Configure HTTP basic auth (disabled, using session-based auth)
            .httpBasic(AbstractHttpConfigurer::disable)

            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    /**
     * Configure CORS (Cross-Origin Resource Sharing).
     *
     * <p>Allows the Vue frontend to communicate with the backend API.</p>
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins - in production, configure specific domains
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://*.localhost:*"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        // Expose headers that frontend can read
        configuration.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    /**
     * Configure the password encoder.
     *
     * <p>Uses BCrypt with default strength (10 rounds).</p>
     *
     * @return the BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
