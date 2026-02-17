package org.example.appointment_system.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointment_system.config.SecurityConfig;
import org.example.appointment_system.dto.request.LoginRequest;
import org.example.appointment_system.dto.request.RegisterRequest;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.UserRepository;
import org.example.appointment_system.security.CustomUserDetailsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import redis.embedded.RedisServer;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests using H2 in-memory database.
 * Provides common test utilities for full stack integration testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Import({SecurityConfig.class, CustomUserDetailsService.class})
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public abstract class BaseIntegrationTest {

    private static RedisServer redisServer;

    protected MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    protected String sessionId;

    @BeforeAll
    static void startRedis() {
        redisServer = new RedisServer(6379);
        try {
            redisServer.start();
        } catch (Exception e) {
            // Redis might already be running, which is fine
        }
    }

    @AfterAll
    static void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @BeforeEach
    void setUp() {
        // Setup MockMvc with security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clear Redis before each test
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Redis might not be available in some test configurations
        }
    }

    /**
     * Generate a unique username for tests.
     */
    protected String generateUniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate a unique email for tests.
     */
    protected String generateUniqueEmail(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    /**
     * Register a new user and return the result.
     */
    protected MvcResult registerUser(String username, String password, String email, UserRole role) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        request.setRole(role);

        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    /**
     * Login and store session ID.
     */
    protected MvcResult login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract session cookie
        jakarta.servlet.http.Cookie[] cookies = result.getResponse().getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("SESSION".equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Register and login a user with unique credentials, returning the session ID.
     */
    protected String registerAndLoginUnique(String prefix, String password, UserRole role) throws Exception {
        String username = generateUniqueUsername(prefix);
        String email = generateUniqueEmail(prefix);
        registerUser(username, password, email, role);
        login(username, password);
        return sessionId;
    }

    /**
     * Create a test user directly in the database.
     */
    protected User createTestUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    /**
     * Extract value from JSON response.
     */
    protected String extractJsonValue(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.has(field) ? node.get(field).asText() : null;
    }

    /**
     * Extract numeric value from JSON response.
     */
    protected Long extractJsonLong(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.has(field) ? node.get(field).asLong() : null;
    }
}
