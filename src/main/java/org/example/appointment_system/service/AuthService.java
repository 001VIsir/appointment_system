package org.example.appointment_system.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.LoginRequest;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
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
 * Service for authentication operations.
 *
 * <p>Handles user registration, login, and session management.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>User registration with validation</li>
 *   <li>Username and email uniqueness checks</li>
 *   <li>BCrypt password hashing</li>
 *   <li>Role assignment with security checks</li>
 *   <li>Session-based authentication</li>
 *   <li>Login and logout management</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     *
     * <p>Creates a new user account with the following validations:</p>
     * <ul>
     *   <li>Username must be unique</li>
     *   <li>Email must be unique</li>
     *   <li>Password is hashed using BCrypt</li>
     *   <li>Role defaults to USER if not specified</li>
     * </ul>
     *
     * @param request the registration request containing user details
     * @return UserResponse containing the created user's information
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Attempting to register user with username: {}", request.getUsername());

        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' already exists", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email '{}' already exists", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Determine role - default to USER
        UserRole role = request.getRole();
        if (role == null) {
            role = UserRole.USER;
        }

        // Create user entity with hashed password
        User user = new User(
            request.getUsername(),
            passwordEncoder.encode(request.getPassword()),
            request.getEmail(),
            role
        );

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, username={}, role={}",
            savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        // Return response (without password)
        return mapToResponse(savedUser);
    }

    /**
     * Check if a username is available.
     *
     * @param username the username to check
     * @return true if available, false if already taken
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Check if an email is available.
     *
     * @param email the email to check
     * @return true if available, false if already taken
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Authenticate a user and create a session.
     *
     * <p>Validates credentials and creates a Spring Security session.</p>
     *
     * @param request the login request containing credentials
     * @param httpRequest the HTTP request for session creation
     * @return UserResponse containing the authenticated user's information
     * @throws BadCredentialsException if username or password is invalid
     * @throws DisabledException if the user account is disabled
     */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Attempting to login user: {}", request.getUsername());

        try {
            // Authenticate using Spring Security
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            Authentication authentication = authenticationManager.authenticate(authToken);

            // Get the authenticated user
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Create session and set security context
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
     * Logout the current user by invalidating the session.
     *
     * @param httpRequest the HTTP request containing the session
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
     * Get the currently authenticated user.
     *
     * @return Optional containing UserResponse if authenticated, empty otherwise
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

        // Handle anonymous user
        if ("anonymousUser".equals(username)) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username)
            .map(this::mapToResponse);
    }

    /**
     * Get username from session for logging purposes.
     *
     * @param session the HTTP session
     * @return the username or "unknown"
     */
    private String getUsernameFromSession(HttpSession session) {
        SecurityContext context = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        if (context != null && context.getAuthentication() != null) {
            return context.getAuthentication().getName();
        }
        return "unknown";
    }

    /**
     * Map User entity to UserResponse DTO.
     *
     * @param user the user entity
     * @return the user response DTO
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
