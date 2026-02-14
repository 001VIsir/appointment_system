package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.UserRole;

import java.time.LocalDateTime;

/**
 * DTO for user response data.
 *
 * <p>Excludes sensitive information like password.
 * Used for API responses containing user information.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /**
     * Unique identifier of the user.
     */
    private Long id;

    /**
     * Username of the account.
     */
    private String username;

    /**
     * Email address of the user.
     */
    private String email;

    /**
     * Role assigned to the user.
     */
    private UserRole role;

    /**
     * Whether the account is enabled.
     */
    private boolean enabled;

    /**
     * Timestamp when the account was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the account was last updated.
     */
    private LocalDateTime updatedAt;
}
