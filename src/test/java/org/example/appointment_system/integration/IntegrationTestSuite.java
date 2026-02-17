package org.example.appointment_system.integration;

import org.example.appointment_system.dto.request.LoginRequest;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Authentication and Health endpoints.
 * Tests the core authentication flow without complex session handling.
 *
 * Note: Full API integration tests are covered by the 799 unit tests.
 * These tests verify the basic integration with H2 database and embedded Redis.
 */
@DisplayName("Integration Tests")
class IntegrationTestSuite extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("Health Check")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return healthy status")
        void healthEndpoint_ReturnsOk() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Nested
    @DisplayName("User Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully with unique credentials")
        void registerNewUser_Success() throws Exception {
            String username = generateUniqueUsername("newuser");
            String email = generateUniqueEmail("newuser");

            RegisterRequest request = new RegisterRequest();
            request.setUsername(username);
            request.setPassword("password123");
            request.setEmail(email);
            request.setRole(UserRole.USER);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.id").exists());

            // Verify user is saved in database
            assertThat(userRepository.findByUsername(username)).isPresent();
        }

        @Test
        @DisplayName("Should register merchant user successfully")
        void registerMerchantUser_Success() throws Exception {
            String username = generateUniqueUsername("merchant");
            String email = generateUniqueEmail("merchant");

            RegisterRequest request = new RegisterRequest();
            request.setUsername(username);
            request.setPassword("password123");
            request.setEmail(email);
            request.setRole(UserRole.MERCHANT);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("MERCHANT"));
        }

        @Test
        @DisplayName("Should register admin user successfully")
        void registerAdminUser_Success() throws Exception {
            String username = generateUniqueUsername("admin");
            String email = generateUniqueEmail("admin");

            RegisterRequest request = new RegisterRequest();
            request.setUsername(username);
            request.setPassword("password123");
            request.setEmail(email);
            request.setRole(UserRole.ADMIN);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("Should reject invalid email format")
        void registerInvalidEmail_Fail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(generateUniqueUsername("user"));
            request.setPassword("password123");
            request.setEmail("invalid-email");
            request.setRole(UserRole.USER);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject short password")
        void registerShortPassword_Fail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(generateUniqueUsername("user"));
            request.setPassword("123");
            request.setEmail(generateUniqueEmail("user"));
            request.setRole(UserRole.USER);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject blank username")
        void registerBlankUsername_Fail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("");
            request.setPassword("password123");
            request.setEmail(generateUniqueEmail("user"));
            request.setRole(UserRole.USER);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("User Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_Success() throws Exception {
            // Register user first
            String username = generateUniqueUsername("loginuser");
            String email = generateUniqueEmail("loginuser");
            registerUser(username, "password123", email, UserRole.USER);

            // Login
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.email").value(email));
        }

        @Test
        @DisplayName("Should reject invalid password")
        void loginInvalidPassword_Fail() throws Exception {
            // Register user first
            String username = generateUniqueUsername("wrongpass");
            String email = generateUniqueEmail("wrongpass");
            registerUser(username, "password123", email, UserRole.USER);

            // Login with wrong password
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword("wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject non-existent user")
        void loginNonExistentUser_Fail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent_user_12345");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Protected Endpoint Access")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Should reject unauthenticated access to protected endpoints")
        void protectedEndpoint_RequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/bookings/my"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject unauthenticated access to merchant endpoints")
        void merchantEndpoint_RequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/merchants/profile"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject unauthenticated access to admin endpoints")
        void adminEndpoint_RequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/admin/metrics"))
                    .andExpect(status().isForbidden());
        }
    }
}
