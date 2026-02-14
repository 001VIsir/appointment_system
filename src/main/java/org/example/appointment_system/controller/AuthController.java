package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * REST Controller for authentication operations.
 *
 * <p>Provides endpoints for user registration, login, and logout.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>POST /api/auth/register - Register a new user (public)</li>
 *   <li>POST /api/auth/login - Login (public, handled by Spring Security)</li>
 *   <li>POST /api/auth/logout - Logout (requires authentication)</li>
 *   <li>GET /api/auth/me - Get current user (requires authentication)</li>
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
     * Register a new user.
     *
     * <p>Creates a new user account. This endpoint is publicly accessible.</p>
     *
     * @param request the registration request
     * @return the created user information
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
     * Check if a username is available.
     *
     * @param username the username to check
     * @return true if available, false otherwise
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
     * Check if an email is available.
     *
     * @param email the email to check
     * @return true if available, false otherwise
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
}
