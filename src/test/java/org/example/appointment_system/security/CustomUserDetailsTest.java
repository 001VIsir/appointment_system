package org.example.appointment_system.security;

import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CustomUserDetails.
 */
class CustomUserDetailsTest {

    private User testUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "hashedPassword123", "test@example.com", UserRole.USER);
        testUser.setId(1L);
        testUser.setEnabled(true);
        userDetails = new CustomUserDetails(testUser);
    }

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorTests {

        @Test
        @DisplayName("should create CustomUserDetails from User entity")
        void constructor_shouldCreateFromUserEntity() {
            // Then
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getId()).isEqualTo(1L);
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getPassword()).isEqualTo("hashedPassword123");
            assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
            assertThat(userDetails.getRole()).isEqualTo(UserRole.USER);
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should provide access to underlying User entity")
        void getUser_shouldReturnUnderlyingEntity() {
            // When
            User user = userDetails.getUser();

            // Then
            assertThat(user).isNotNull();
            assertThat(user).isSameAs(testUser);
        }
    }

    @Nested
    @DisplayName("getAuthorities() method")
    class GetAuthoritiesTests {

        @Test
        @DisplayName("should return ROLE_USER for USER role")
        void getAuthorities_withUserRole_shouldReturnRoleUser() {
            // When
            var authorities = userDetails.getAuthorities();

            // Then
            assertThat(authorities)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should return ROLE_ADMIN for ADMIN role")
        void getAuthorities_withAdminRole_shouldReturnRoleAdmin() {
            // Given
            User adminUser = new User("admin", "password", "admin@example.com", UserRole.ADMIN);
            CustomUserDetails adminDetails = new CustomUserDetails(adminUser);

            // When
            var authorities = adminDetails.getAuthorities();

            // Then
            assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should return ROLE_MERCHANT for MERCHANT role")
        void getAuthorities_withMerchantRole_shouldReturnRoleMerchant() {
            // Given
            User merchantUser = new User("merchant", "password", "merchant@example.com", UserRole.MERCHANT);
            CustomUserDetails merchantDetails = new CustomUserDetails(merchantUser);

            // When
            var authorities = merchantDetails.getAuthorities();

            // Then
            assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MERCHANT");
        }
    }

    @Nested
    @DisplayName("Account Status Methods")
    class AccountStatusTests {

        @Test
        @DisplayName("should return true for isAccountNonExpired")
        void isAccountNonExpired_shouldReturnTrue() {
            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return true for isAccountNonLocked")
        void isAccountNonLocked_shouldReturnTrue() {
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should return true for isCredentialsNonExpired")
        void isCredentialsNonExpired_shouldReturnTrue() {
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return true for isEnabled when user is enabled")
        void isEnabled_whenUserEnabled_shouldReturnTrue() {
            testUser.setEnabled(true);
            CustomUserDetails enabledDetails = new CustomUserDetails(testUser);

            assertThat(enabledDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return false for isEnabled when user is disabled")
        void isEnabled_whenUserDisabled_shouldReturnFalse() {
            testUser.setEnabled(false);
            CustomUserDetails disabledDetails = new CustomUserDetails(testUser);

            assertThat(disabledDetails.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Getter Methods")
    class GetterTests {

        @Test
        @DisplayName("should return correct id")
        void getId_shouldReturnCorrectId() {
            assertThat(userDetails.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return correct username")
        void getUsername_shouldReturnCorrectUsername() {
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should return correct password")
        void getPassword_shouldReturnCorrectPassword() {
            assertThat(userDetails.getPassword()).isEqualTo("hashedPassword123");
        }

        @Test
        @DisplayName("should return correct email")
        void getEmail_shouldReturnCorrectEmail() {
            assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return correct role")
        void getRole_shouldReturnCorrectRole() {
            assertThat(userDetails.getRole()).isEqualTo(UserRole.USER);
        }
    }
}
