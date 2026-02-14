package org.example.appointment_system.enums;

/**
 * User role enumeration for role-based access control.
 *
 * <p>Defines the three types of users in the appointment system:</p>
 * <ul>
 *   <li>{@link #ADMIN} - System administrators with full access</li>
 *   <li>{@link #MERCHANT} - Business owners who create appointment tasks</li>
 *   <li>{@link #USER} - End users who book appointments</li>
 * </ul>
 */
public enum UserRole {

    /**
     * Administrator role with full system access.
     * Can manage all users, merchants, and system settings.
     */
    ADMIN("ROLE_ADMIN", "Administrator"),

    /**
     * Merchant role for business owners.
     * Can create service items, appointment tasks, and manage their bookings.
     */
    MERCHANT("ROLE_MERCHANT", "Merchant"),

    /**
     * Standard user role for appointment booking.
     * Can view available tasks and create bookings.
     */
    USER("ROLE_USER", "User");

    private final String authority;
    private final String displayName;

    UserRole(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }

    /**
     * Get the Spring Security authority name.
     *
     * @return the authority string (e.g., "ROLE_ADMIN")
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Get the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
