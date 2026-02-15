package org.example.appointment_system.security;

import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomUserDetailsService.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "hashedPassword", "test@example.com", UserRole.USER);
        testUser.setId(1L);
        testUser.setEnabled(true);
    }

    @Nested
    @DisplayName("loadUserByUsername() method")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("should load user successfully when username exists")
        void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.getId()).isEqualTo(1L);
            assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
            assertThat(userDetails.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("should return correct authorities for USER role")
        void loadUserByUsername_withUserRole_shouldHaveCorrectAuthority() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should return correct authorities for ADMIN role")
        void loadUserByUsername_withAdminRole_shouldHaveCorrectAuthority() {
            // Given
            User adminUser = new User("adminuser", "hashedPassword", "admin@example.com", UserRole.ADMIN);
            adminUser.setId(2L);
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(adminUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("adminuser");

            // Then
            assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should return correct authorities for MERCHANT role")
        void loadUserByUsername_withMerchantRole_shouldHaveCorrectAuthority() {
            // Given
            User merchantUser = new User("merchantuser", "hashedPassword", "merchant@example.com", UserRole.MERCHANT);
            merchantUser.setId(3L);
            when(userRepository.findByUsername("merchantuser")).thenReturn(Optional.of(merchantUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("merchantuser");

            // Then
            assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MERCHANT");
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user not found")
        void loadUserByUsername_whenUserNotFound_shouldThrowException() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: nonexistent");
        }

        @Test
        @DisplayName("should return account non expired as true")
        void loadUserByUsername_shouldReturnAccountNonExpired() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return account non locked as true")
        void loadUserByUsername_shouldReturnAccountNonLocked() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should return credentials non expired as true")
        void loadUserByUsername_shouldReturnCredentialsNonExpired() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return enabled false when user is disabled")
        void loadUserByUsername_whenUserDisabled_shouldReturnEnabledFalse() {
            // Given
            testUser.setEnabled(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should provide access to underlying User entity")
        void loadUserByUsername_shouldProvideAccessToUserEntity() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.getUser()).isNotNull();
            assertThat(userDetails.getUser().getId()).isEqualTo(1L);
            assertThat(userDetails.getUser().getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getUser().getEmail()).isEqualTo("test@example.com");
        }
    }
}
