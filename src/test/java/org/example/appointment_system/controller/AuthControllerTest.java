package org.example.appointment_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.dto.response.UserResponse;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController using standalone MockMvc setup.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest validRequest;
    private UserResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization

        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");
        validRequest.setRole(null);

        validResponse = UserResponse.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .role(UserRole.USER)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("should register user successfully and return 201")
        void register_withValidRequest_shouldReturn201() throws Exception {
            // Given
            when(authService.register(any(RegisterRequest.class))).thenReturn(validResponse);

            // When/Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true));
        }

        // Note: Tests for existing username/email validation require GlobalExceptionHandler
        // which will be implemented in FEAT-025. These tests will be added then.
    }

    @Nested
    @DisplayName("GET /api/auth/check-username")
    class CheckUsernameEndpointTests {

        @Test
        @DisplayName("should return true when username is available")
        void checkUsername_whenAvailable_returnsTrue() throws Exception {
            // Given
            when(authService.isUsernameAvailable("newuser")).thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/auth/check-username")
                    .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("should return false when username is taken")
        void checkUsername_whenTaken_returnsFalse() throws Exception {
            // Given
            when(authService.isUsernameAvailable("existinguser")).thenReturn(false);

            // When/Then
            mockMvc.perform(get("/api/auth/check-username")
                    .param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/check-email")
    class CheckEmailEndpointTests {

        @Test
        @DisplayName("should return true when email is available")
        void checkEmail_whenAvailable_returnsTrue() throws Exception {
            // Given
            when(authService.isEmailAvailable("new@example.com")).thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/auth/check-email")
                    .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("should return false when email is taken")
        void checkEmail_whenTaken_returnsFalse() throws Exception {
            // Given
            when(authService.isEmailAvailable("existing@example.com")).thenReturn(false);

            // When/Then
            mockMvc.perform(get("/api/auth/check-email")
                    .param("email", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }
    }
}
