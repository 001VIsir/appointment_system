package org.example.appointment_system.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of Spring Security UserDetailsService.
 *
 * <p>Loads user details from the database for authentication.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Loads users by username from the database</li>
 *   <li>Wraps User entity in CustomUserDetails for Spring Security</li>
 *   <li>Provides transactional read-only access</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by username for authentication.
     *
     * <p>This method is called by Spring Security during the authentication process
     * to retrieve user details for credential validation.</p>
     *
     * @param username the username to look up
     * @return UserDetails containing the user's authentication information
     * @throws UsernameNotFoundException if the user is not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("User not found with username: {}", username);
                return new UsernameNotFoundException("User not found with username: " + username);
            });

        log.debug("User loaded successfully: id={}, username={}, role={}",
            user.getId(), user.getUsername(), user.getRole());

        return new CustomUserDetails(user);
    }
}
