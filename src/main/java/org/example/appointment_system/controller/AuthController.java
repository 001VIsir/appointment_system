package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.LoginRequest;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证操作的REST控制器。
 *
 * <p>提供用户注册、登录和登出的接口。</p>
 *
 * <h3>接口列表：</h3>
 * <ul>
 *   <li>POST /api/auth/register - 注册新用户（公开）</li>
 *   <li>POST /api/auth/login - 登录（公开，由Spring Security处理）</li>
 *   <li>POST /api/auth/logout - 登出（需要认证）</li>
 *   <li>GET /api/auth/me - 获取当前用户（需要认证）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    /**
     * 注册新用户。
     *
     * <p>创建新的用户账户。此接口公开可访问。</p>
     *
     * @param request 注册请求
     * @return 创建的用户信息
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Username and email must be unique.",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "User registered successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request or username/email already exists"
            )
        }
    )
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 检查用户名是否可用。
     *
     * @param username 要检查的用户名
     * @return 可用返回true，否则返回false
     */
    @GetMapping("/check-username")
    @Operation(
        summary = "Check username availability",
        description = "Checks if the given username is available for registration"
    )
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        return ResponseEntity.ok(available);
    }

    /**
     * 检查邮箱是否可用。
     *
     * @param email 要检查的邮箱
     * @return 可用返回true，否则返回false
     */
    @GetMapping("/check-email")
    @Operation(
        summary = "Check email availability",
        description = "Checks if the given email is available for registration"
    )
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(available);
    }

    /**
     * 用户登录。
     *
     * <p>验证用户凭据并创建会话。此接口公开可访问。</p>
     *
     * @param request 包含凭据的登录请求
     * @param httpRequest 用于创建会话的HTTP请求
     * @return 已认证的用户信息
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticates user credentials and creates a session. Session is stored in Redis.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Invalid credentials or account disabled"
            )
        }
    )
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        log.info("Login request received for username: {}", request.getUsername());
        UserResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 登出当前用户。
     *
     * <p>使当前会话失效。需要认证。</p>
     *
     * @param httpRequest 包含会话的HTTP请求
     * @return 空的204状态响应
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates the current session and logs out the user"
    )
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        log.info("Logout request received");
        authService.logout(httpRequest);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取当前已认证的用户。
     *
     * <p>需要认证。返回当前已登录用户的信息。</p>
 *
     * @return 当前用户信息，未认证则返回401
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Returns the currently authenticated user's information",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Current user information",
                content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Not authenticated"
            )
        }
    )
    public ResponseEntity<UserResponse> getCurrentUser() {
        return authService.getCurrentUser()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
