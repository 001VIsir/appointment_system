package org.example.appointment_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for user login request.
 *
 * <p>Contains credentials for authenticating a user.</p>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>Username: required, 3-50 characters</li>
 *   <li>Password: required, minimum 6 characters</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    /**
     * Username for authentication.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Password for authentication.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
