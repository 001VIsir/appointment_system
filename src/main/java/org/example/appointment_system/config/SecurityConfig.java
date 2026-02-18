package org.example.appointment_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
 * Spring Security配置类。
 *
 * <p>此配置为预约系统设置基础安全基础设施，
 * 包括基于会话的认证、CORS策略和基于路径的授权规则。</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>存储在Redis中的基于会话的认证</li>
 *   <li>前端集成的CORS配置</li>
 *   <li>为无状态API访问禁用CSRF保护</li>
 *   <li>基于路径的授权规则</li>
 *   <li>BCrypt密码编码</li>
 *   <li>用于编程式登录的认证管理器</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * 配置安全过滤器链。
     *
     * <p>定义HTTP请求的安全规则，包括：</p>
     * <ul>
     *   <li>公开接口：健康检查、OpenAPI文档、公开预约页面</li>
     *   <li>受保护接口：需要认证</li>
     *   <li>管理员接口：需要ADMIN角色</li>
     * </ul>
     *
     * @param http 要配置的HttpSecurity
     * @return 配置好的SecurityFilterChain
     * @throws Exception 配置失败
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 禁用CSRF（使用同站cookie的基于会话的认证）
            .csrf(AbstractHttpConfigurer::disable)

            // 配置会话管理 - 使用Redis存储的会话
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )

            // 配置授权规则
            .authorizeHttpRequests(authz -> authz
                // 公开接口 - 不需要认证
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
                    "/api/tasks/**",  // 签名链接访问的公开任务查看
                    "/api/search/**", // 公开搜索接口
                    "/error"
                ).permitAll()

                // 管理员接口 - 需要ADMIN角色
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 商家接口 - 需要MERCHANT角色
                .requestMatchers("/api/merchants/**").hasAnyRole("MERCHANT", "ADMIN")

                // 所有其他接口需要认证
                .anyRequest().authenticated()
            )

            // 配置表单登录（禁用，仅API使用）
            .formLogin(AbstractHttpConfigurer::disable)

            // 配置HTTP基本认证（禁用，使用基于会话的认证）
            .httpBasic(AbstractHttpConfigurer::disable)

            // 配置登出
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("SESSION")
            );

        return http.build();
    }

    /**
     * 配置CORS（跨域资源共享）。
     *
     * <p>允许Vue前端与后端API通信。</p>
     *
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源 - 生产环境中配置特定域名
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://*.localhost:*"
        ));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 允许的头部
        configuration.setAllowedHeaders(List.of("*"));

        // 允许凭证（cookie、授权头）
        configuration.setAllowCredentials(true);

        // 预检响应缓存1小时
        configuration.setMaxAge(3600L);

        // 暴露前端可读取的头部
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
     * 配置密码编码器。
     *
     * <p>使用默认强度（10轮）的BCrypt。</p>
     *
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 将AuthenticationManager暴露为Bean。
     *
     * <p>AuthService中编程式认证所需。</p>
     *
     * @param authenticationConfiguration 认证配置
     * @return AuthenticationManager
     * @throws Exception 配置失败
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
