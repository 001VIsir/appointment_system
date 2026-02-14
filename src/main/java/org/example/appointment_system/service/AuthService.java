package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
