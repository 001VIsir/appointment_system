package org.example.appointment_system.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.appointment_system.dto.request.LoginRequest;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.UserRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

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
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private LoginRequest validLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");
        validRequest.setRole(null);

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        testUser = new User("testuser", "hashedPassword", "test@example.com", UserRole.USER);
        testUser.setId(1L);
        testUser.setEnabled(true);

        // Clear security context before each test
        SecurityContextHolder.clearContext();
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

    @Nested
    @DisplayName("login() method")
    class LoginTests {

        @Test
        @DisplayName("should login user successfully with valid credentials")
        void login_withValidCredentials_shouldSucceed() {
            // Given
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(httpRequest.getSession(true)).thenReturn(session);

            // When
            UserResponse response = authService.login(validLoginRequest, httpRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getRole()).isEqualTo(UserRole.USER);
            assertThat(response.isEnabled()).isTrue();

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(session).setAttribute(anyString(), any(SecurityContext.class));
        }

        @Test
        @DisplayName("should throw BadCredentialsException for invalid credentials")
        void login_withInvalidCredentials_shouldThrowException() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            // When/Then
            assertThatThrownBy(() -> authService.login(validLoginRequest, httpRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("should throw DisabledException for disabled account")
        void login_withDisabledAccount_shouldThrowException() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("Account is disabled"));

            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            // When/Then
            assertThatThrownBy(() -> authService.login(validLoginRequest, httpRequest))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("Account is disabled");
        }
    }

    @Nested
    @DisplayName("logout() method")
    class LogoutTests {

        @Test
        @DisplayName("should invalidate session when session exists")
        void logout_withExistingSession_shouldInvalidate() {
            // Given
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);

            when(httpRequest.getSession(false)).thenReturn(session);
            when(session.getAttribute(anyString())).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            // When
            authService.logout(httpRequest);

            // Then
            verify(session).invalidate();
        }

        @Test
        @DisplayName("should handle logout when no session exists")
        void logout_withoutSession_shouldNotThrow() {
            // Given
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getSession(false)).thenReturn(null);

            // When/Then - should not throw
            assertThatCode(() -> authService.logout(httpRequest))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getCurrentUser() method")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return current user when authenticated")
        void getCurrentUser_whenAuthenticated_shouldReturnUser() {
            // Given
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            CustomUserDetails userDetails = new CustomUserDetails(testUser);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When
            Optional<UserResponse> result = authService.getCurrentUser();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return empty when not authenticated")
        void getCurrentUser_whenNotAuthenticated_shouldReturnEmpty() {
            // Given
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            // When
            Optional<UserResponse> result = authService.getCurrentUser();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when authentication is not authenticated")
        void getCurrentUser_whenAuthenticationNotAuthenticated_shouldReturnEmpty() {
            // Given
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);
            SecurityContextHolder.setContext(securityContext);

            // When
            Optional<UserResponse> result = authService.getCurrentUser();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for anonymous user")
        void getCurrentUser_whenAnonymousUser_shouldReturnEmpty() {
            // Given
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            SecurityContextHolder.setContext(securityContext);

            // When
            Optional<UserResponse> result = authService.getCurrentUser();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
