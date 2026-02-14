package org.example.appointment_system.entity;

import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the User entity.
 */
class UserTest {

    @Test
    void testUserCreationWithConstructor() {
        // Arrange & Act
        User user = new User("testuser", "hashedpassword", "test@example.com", UserRole.USER);

        // Assert
        assertEquals("testuser", user.getUsername());
        assertEquals("hashedpassword", user.getPassword());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.isEnabled());
        assertNull(user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    void testUserCreationWithNoArgsConstructor() {
        // Arrange & Act
        User user = new User();

        // Assert
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getEmail());
        assertNull(user.getRole());
        assertTrue(user.isEnabled()); // default value
    }

    @Test
    void testPrePersistSetsTimestamps() {
        // Arrange
        User user = new User("testuser", "hashedpassword", "test@example.com", UserRole.MERCHANT);

        // Act
        user.onCreate();

        // Assert
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    void testPrePersistSetsDefaultRole() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("hashedpassword");
        user.setEmail("test@example.com");
        // role is null

        // Act
        user.onCreate();

        // Assert
        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingRole() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("hashedpassword");
        user.setEmail("test@example.com");
        user.setRole(UserRole.ADMIN);

        // Act
        user.onCreate();

        // Assert
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    void testPreUpdateSetsUpdatedAt() {
        // Arrange
        User user = new User("testuser", "hashedpassword", "test@example.com", UserRole.USER);
        user.onCreate();
        LocalDateTime originalCreatedAt = user.getCreatedAt();

        // Wait a tiny bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        // Act
        user.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, user.getCreatedAt()); // createdAt should not change
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(originalCreatedAt) || user.getUpdatedAt().equals(originalCreatedAt));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        User user = new User();

        // Act
        user.setId(1L);
        user.setUsername("newuser");
        user.setPassword("newpassword");
        user.setEmail("new@example.com");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals(1L, user.getId());
        assertEquals("newuser", user.getUsername());
        assertEquals("newpassword", user.getPassword());
        assertEquals("new@example.com", user.getEmail());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertFalse(user.isEnabled());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void testEnabledDefaultValue() {
        // Arrange & Act
        User user = new User();

        // Assert
        assertTrue(user.isEnabled());
    }

    @Test
    void testEnabledCanBeSetToFalse() {
        // Arrange
        User user = new User();

        // Act
        user.setEnabled(false);

        // Assert
        assertFalse(user.isEnabled());
    }

    @Test
    void testAllRolesAreAvailable() {
        // Arrange & Act & Assert
        User admin = new User("admin", "pass", "admin@test.com", UserRole.ADMIN);
        User merchant = new User("merchant", "pass", "merchant@test.com", UserRole.MERCHANT);
        User regularUser = new User("user", "pass", "user@test.com", UserRole.USER);

        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals(UserRole.MERCHANT, merchant.getRole());
        assertEquals(UserRole.USER, regularUser.getRole());
    }
}
