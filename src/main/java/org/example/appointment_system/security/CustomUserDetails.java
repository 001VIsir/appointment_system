package org.example.appointment_system.security;

import lombok.Getter;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security UserDetails.
 *
 * <p>Wraps the User entity to provide authentication details to Spring Security.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Maps User entity to Spring Security UserDetails</li>
 *   <li>Converts UserRole to GrantedAuthority</li>
 *   <li>Provides access to underlying User entity</li>
 * </ul>
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final UserRole role;
    private final boolean enabled;
    private final User user;

    /**
     * Construct CustomUserDetails from User entity.
     *
     * @param user the User entity
     */
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
