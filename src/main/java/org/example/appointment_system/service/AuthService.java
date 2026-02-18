package org.example.appointment_system.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.LoginRequest;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

/**
 * 认证操作服务类。
 *
 * <p>处理用户注册、登录和会话管理。</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>带验证的用户注册</li>
 *   <li>用户名和邮箱唯一性检查</li>
 *   <li>BCrypt密码哈希</li>
 *   <li>带安全检查的角色分配</li>
 *   <li>基于会话的认证</li>
 *   <li>登录和登出管理</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * 注册新用户。
     *
     * <p>创建新用户账户，包含以下验证：</p>
     * <ul>
     *   <li>用户名必须唯一</li>
     *   <li>邮箱必须唯一</li>
     *   <li>密码使用BCrypt哈希</li>
     *   <li>如未指定角色，默认为USER</li>
     * </ul>
     *
     * @param request 包含用户详情的注册请求
     * @return 包含已创建用户信息的UserResponse
     * @throws IllegalArgumentException 如果用户名或邮箱已存在
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Attempting to register user with username: {}", request.getUsername());

        // 验证用户名唯一性
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' already exists", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // 验证邮箱唯一性
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email '{}' already exists", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // 确定角色 - 默认为USER
        UserRole role = request.getRole();
        if (role == null) {
            role = UserRole.USER;
        }

        // 创建带有哈希密码的用户实体
        User user = new User(
            request.getUsername(),
            passwordEncoder.encode(request.getPassword()),
            request.getEmail(),
            role
        );

        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, username={}, role={}",
            savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        // 如果是商户角色，自动创建商户资料
        if (role == UserRole.MERCHANT) {
            MerchantProfile profile = new MerchantProfile(
                savedUser,
                savedUser.getUsername() + "的店铺",  // 默认店铺名
                "欢迎使用预约系统",                 // 默认描述
                null,                               // 电话
                null,                               // 地址
                null                                // 设置
            );
            merchantProfileRepository.save(profile);
            log.info("Created default merchant profile for user: {}", savedUser.getUsername());
        }

        // 返回响应（不含密码）
        return mapToResponse(savedUser);
    }

    /**
     * 检查用户名是否可用。
     *
     * @param username 要检查的用户名
     * @return 可用返回true，已占用返回false
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否可用。
     *
     * @param email 要检查的邮箱
     * @return 可用返回true，已占用返回false
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * 验证用户并创建会话。
     *
     * <p>验证凭据并创建Spring Security会话。</p>
     *
     * @param request 包含凭据的登录请求
     * @param httpRequest 用于创建会话的HTTP请求
     * @return 包含已认证用户信息的UserResponse
     * @throws BadCredentialsException 用户名或密码无效
     * @throws DisabledException 用户账户已被禁用
     */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Attempting to login user: {}", request.getUsername());

        try {
            // 使用Spring Security进行认证
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            Authentication authentication = authenticationManager.authenticate(authToken);

            // 获取已认证的用户
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // 创建会话并设置安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);

            log.info("User logged in successfully: id={}, username={}, role={}",
                user.getId(), user.getUsername(), user.getRole());

            return mapToResponse(user);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user '{}': invalid credentials", request.getUsername());
            throw e;
        } catch (DisabledException e) {
            log.warn("Login failed for user '{}': account disabled", request.getUsername());
            throw e;
        }
    }

    /**
     * 通过使会话失效来登出当前用户。
     *
     * @param httpRequest 包含会话的HTTP请求
     */
    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            String username = getUsernameFromSession(session);
            log.info("Logging out user: {}", username);
            session.invalidate();
        }
    }

    /**
     * 获取当前已认证的用户。
     *
     * @return 如果已认证返回包含UserResponse的Optional，否则返回空
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
        } else {
            return Optional.empty();
        }

        // 处理匿名用户
        if ("anonymousUser".equals(username)) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username)
            .map(this::mapToResponse);
    }

    /**
     * 从会话中获取用户名，用于日志记录。
     *
     * @param session HTTP会话
     * @return 用户名或"unknown"
     */
    private String getUsernameFromSession(HttpSession session) {
        SecurityContext context = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        if (context != null && context.getAuthentication() != null) {
            return context.getAuthentication().getName();
        }
        return "unknown";
    }

    /**
     * 将User实体映射到UserResponse DTO。
     *
     * @param user 用户实体
     * @return 用户响应DTO
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .enabled(user.isEnabled())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
