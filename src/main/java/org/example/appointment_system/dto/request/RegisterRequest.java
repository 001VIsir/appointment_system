package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.UserRole;

/**
 * DTO for user registration request.
 *
 * <p>Contains all required fields for creating a new user account.</p>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>Username: 3-50 characters, alphanumeric with underscores</li>
 *   <li>Password: minimum 6 characters</li>
 *   <li>Email: valid email format</li>
 *   <li>Role: optional, defaults to USER</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    /**
     * Desired username for the account.
     * Must be unique in the system.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    /**
     * Password for the account.
     * Will be hashed using BCrypt before storage.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    /**
     * Email address for the account.
     * Must be unique in the system.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * Role for the new user.
     * Optional - defaults to USER if not specified.
     * Only ADMIN can create MERCHANT or ADMIN accounts.
     */
    private UserRole role;
}
