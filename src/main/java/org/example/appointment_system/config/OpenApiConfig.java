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
 * OpenAPI（Swagger）配置，用于预约系统API。
 *
 * <p>配置：</p>
 * <ul>
 *   <li>API元数据（标题、描述、版本、联系人、许可证）</li>
 *   <li>基于会话的认证安全方案</li>
 *   <li>服务器配置</li>
 * </ul>
 *
 * <h3>访问点：</h3>
 * <ul>
 *   <li>Swagger UI: /swagger-ui.html</li>
 *   <li>OpenAPI JSON: /api-docs</li>
 *   <li>OpenAPI YAML: /api-docs.yaml</li>
 * </ul>
 *
 * <h3>API分组：</h3>
 * <p>API使用@Tag注解按功能区域组织：</p>
 * <ul>
 *   <li>Authentication - 用户注册、登录、登出</li>
 *   <li>Merchant - 商家资料和设置</li>
 *   <li>Service Item - 服务目录管理</li>
 *   <li>Appointment Task - 预约任务和时段管理</li>
 *   <li>Booking - 预约创建和管理</li>
 *   <li>Signed Links - 安全链接生成</li>
 *   <li>Admin - 系统统计和管理</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 配置预约系统的OpenAPI规范。
     *
     * @return 配置好的OpenAPI实例
     */
    @Bean
    public OpenAPI appointmentSystemOpenAPI() {
        final String securitySchemeName = "sessionAuth";

        return new OpenAPI()
            // API信息
            .info(apiInfo())
            // 服务器
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("开发服务器"),
                new Server()
                    .url("https://api.appointment-system.example.com")
                    .description("生产服务器")
            ))
            // 安全配置（基于会话）
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, sessionSecurityScheme())
            );
    }

    /**
     * 创建API信息元数据。
     *
     * @return API信息实例
     */
    private Info apiInfo() {
        return new Info()
            .title("预约系统 API")
            .description("""
                ## 预约平台 API

                一个全面的预约预订平台，商户可以创建预约任务，
                用户可以通过安全的签名链接预约服务。

                ### 功能特性
                - **用户管理**：注册、认证、基于角色的访问控制
                - **商户门户**：资料管理、服务目录、预约管理
                - **预约系统**：任务创建、时段管理、基于乐观锁的预约
                - **安全链接**：用于公开预约访问的HMAC签名链接
                - **通知服务**：预约事件日志记录
                - **统计与分析**：预约指标、用户统计、系统监控

                ### 认证方式
                本API使用基于会话的认证。认证步骤：
                1. 使用凭据调用 `POST /api/auth/login`
                2. 会话Cookie将自动包含在后续请求中
                3. 调用 `POST /api/auth/logout` 结束会话

                ### 限流策略
                - 匿名用户：60次/分钟
                - 已认证用户：120次/分钟
                - 认证端点：10次/分钟

                ### 错误处理
                所有错误遵循一致的格式，包含错误代码和描述性消息。
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("预约系统支持")
                .email("support@appointment-system.example.com")
                .url("https://appointment-system.example.com/support"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * 创建基于会话的安全方案。
     *
     * <p>由于应用程序使用Spring Session与Redis，认证通过会话Cookie（SESSION）处理。
     * 此安全方案为API使用者记录了这一要求。</p>
     *
     * @return 会话认证的安全方案
     */
    private SecurityScheme sessionSecurityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .name("SESSION")
            .description("""
                基于Cookie的会话认证。

                认证步骤：
                1. 使用用户名和密码发送POST请求到 /api/auth/login
                2. 服务器将设置一个SESSION cookie
                3. 在后续请求中包含此cookie

                会话超时时间：4小时（可按商户配置）
                """);
    }
}
