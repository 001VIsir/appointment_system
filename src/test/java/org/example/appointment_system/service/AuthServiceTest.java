package org.example.appointment_system.service;

import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");
        validRequest.setRole(null);
    }

    @Nested
    @DisplayName("register() method")
    class RegisterTests {

        @Test
        @DisplayName("should register user successfully with default USER role")
        void register_withValidRequest_shouldSucceed() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

            User savedUser = new User("testuser", "hashedPassword", "test@example.com", UserRole.USER);
            savedUser.setId(1L);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = authService.register(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getRole()).isEqualTo(UserRole.USER);
            assertThat(response.isEnabled()).isTrue();

            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("should register user with MERCHANT role when specified")
        void register_withMerchantRole_shouldSucceed() {
            // Given
            validRequest.setRole(UserRole.MERCHANT);

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

            User savedUser = new User("testuser", "hashedPassword", "test@example.com", UserRole.MERCHANT);
            savedUser.setId(1L);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = authService.register(validRequest);

            // Then
            assertThat(response.getRole()).isEqualTo(UserRole.MERCHANT);
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void register_withExistingUsername_shouldThrowException() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void register_withExistingEmail_shouldThrowException() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should hash password before saving")
        void register_shouldHashPassword() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");

            User savedUser = new User("testuser", "hashedPassword123", "test@example.com", UserRole.USER);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            authService.register(validRequest);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashedPassword123");
        }
    }

    @Nested
    @DisplayName("isUsernameAvailable() method")
    class IsUsernameAvailableTests {

        @Test
        @DisplayName("should return true when username is available")
        void isUsernameAvailable_whenAvailable_returnsTrue() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            // When
            boolean available = authService.isUsernameAvailable("newuser");

            // Then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("should return false when username is taken")
        void isUsernameAvailable_whenTaken_returnsFalse() {
            // Given
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // When
            boolean available = authService.isUsernameAvailable("existinguser");

            // Then
            assertThat(available).isFalse();
        }
    }

    @Nested
    @DisplayName("isEmailAvailable() method")
    class IsEmailAvailableTests {

        @Test
        @DisplayName("should return true when email is available")
        void isEmailAvailable_whenAvailable_returnsTrue() {
            // Given
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            // When
            boolean available = authService.isEmailAvailable("new@example.com");

            // Then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("should return false when email is taken")
        void isEmailAvailable_whenTaken_returnsFalse() {
            // Given
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When
            boolean available = authService.isEmailAvailable("existing@example.com");

            // Then
            assertThat(available).isFalse();
        }
    }
}
