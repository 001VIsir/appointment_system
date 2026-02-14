package org.example.appointment_system.repository;

import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User adminUser;
    private User merchantUser;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User("testuser", "encodedPassword123", "test@example.com", UserRole.USER);
        adminUser = new User("admin", "adminPassword456", "admin@example.com", UserRole.ADMIN);
        merchantUser = new User("merchant", "merchantPassword789", "merchant@example.com", UserRole.MERCHANT);

        // Persist users
        entityManager.persist(testUser);
        entityManager.persist(adminUser);
        entityManager.persist(merchantUser);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and find user by ID")
        void shouldSaveAndFindById() {
            // Arrange
            User newUser = new User("newuser", "password", "new@example.com", UserRole.USER);

            // Act
            User savedUser = userRepository.save(newUser);
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("newuser");
            assertThat(foundUser.get().getEmail()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("Should update user")
        void shouldUpdateUser() {
            // Arrange
            testUser.setEmail("updated@example.com");

            // Act
            User updatedUser = userRepository.save(testUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<User> foundUser = userRepository.findById(updatedUser.getId());
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("updated@example.com");
        }

        @Test
        @DisplayName("Should delete user")
        void shouldDeleteUser() {
            // Act
            userRepository.delete(testUser);
            entityManager.flush();

            // Assert
            Optional<User> deletedUser = userRepository.findById(testUser.getId());
            assertThat(deletedUser).isEmpty();
        }

        @Test
        @DisplayName("Should find all users")
        void shouldFindAll() {
            // Act
            List<User> users = userRepository.findAll();

            // Assert
            assertThat(users).hasSize(3);
        }

        @Test
        @DisplayName("Should count users")
        void shouldCount() {
            // Act
            long count = userRepository.count();

            // Assert
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Find by Username")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should find user by existing username")
        void shouldFindByExistingUsername() {
            // Act
            Optional<User> foundUser = userRepository.findByUsername("testuser");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
            assertThat(foundUser.get().getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("Should return empty for non-existing username")
        void shouldReturnEmptyForNonExistingUsername() {
            // Act
            Optional<User> foundUser = userRepository.findByUsername("nonexistent");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should be case-sensitive for username")
        void shouldBeCaseSensitiveForUsername() {
            // Act
            Optional<User> foundUser = userRepository.findByUsername("TESTUSER");

            // Assert
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by Email")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by existing email")
        void shouldFindByExistingEmail() {
            // Act
            Optional<User> foundUser = userRepository.findByEmail("test@example.com");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should return empty for non-existing email")
        void shouldReturnEmptyForNonExistingEmail() {
            // Act
            Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should be case-sensitive for email")
        void shouldBeCaseSensitiveForEmail() {
            // Act
            Optional<User> foundUser = userRepository.findByEmail("TEST@EXAMPLE.COM");

            // Assert
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists by Username/Email")
    class ExistsTests {

        @Test
        @DisplayName("Should return true for existing username")
        void shouldReturnTrueForExistingUsername() {
            // Act
            boolean exists = userRepository.existsByUsername("testuser");

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existing username")
        void shouldReturnFalseForNonExistingUsername() {
            // Act
            boolean exists = userRepository.existsByUsername("nonexistent");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true for existing email")
        void shouldReturnTrueForExistingEmail() {
            // Act
            boolean exists = userRepository.existsByEmail("test@example.com");

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existing email")
        void shouldReturnFalseForNonExistingEmail() {
            // Act
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Find by Role")
    class FindByRoleTests {

        @Test
        @DisplayName("Should find users by ADMIN role")
        void shouldFindUsersByAdminRole() {
            // Act
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);

            // Assert
            assertThat(admins).hasSize(1);
            assertThat(admins.get(0).getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Should find users by USER role")
        void shouldFindUsersByUserRole() {
            // Act
            List<User> users = userRepository.findByRole(UserRole.USER);

            // Assert
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should find users by MERCHANT role")
        void shouldFindUsersByMerchantRole() {
            // Act
            List<User> merchants = userRepository.findByRole(UserRole.MERCHANT);

            // Assert
            assertThat(merchants).hasSize(1);
            assertThat(merchants.get(0).getUsername()).isEqualTo("merchant");
        }

        @Test
        @DisplayName("Should return empty list for role with no users")
        void shouldReturnEmptyListForRoleWithNoUsers() {
            // Arrange - delete all users first
            userRepository.deleteAll();
            entityManager.flush();

            // Act
            List<User> users = userRepository.findByRole(UserRole.USER);

            // Assert
            assertThat(users).isEmpty();
        }

        @Test
        @DisplayName("Should count users by role")
        void shouldCountUsersByRole() {
            // Act
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            long userCount = userRepository.countByRole(UserRole.USER);
            long merchantCount = userRepository.countByRole(UserRole.MERCHANT);

            // Assert
            assertThat(adminCount).isEqualTo(1);
            assertThat(userCount).isEqualTo(1);
            assertThat(merchantCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Find by Enabled Status")
    class FindByEnabledTests {

        @Test
        @DisplayName("Should find enabled users")
        void shouldFindEnabledUsers() {
            // Act
            List<User> enabledUsers = userRepository.findByEnabledTrue();

            // Assert
            assertThat(enabledUsers).hasSize(3);
        }

        @Test
        @DisplayName("Should find disabled users")
        void shouldFindDisabledUsers() {
            // Arrange
            testUser.setEnabled(false);
            entityManager.flush();

            // Act
            List<User> disabledUsers = userRepository.findByEnabledFalse();

            // Assert
            assertThat(disabledUsers).hasSize(1);
            assertThat(disabledUsers.get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should find users by role and enabled status")
        void shouldFindUsersByRoleAndEnabled() {
            // Arrange
            testUser.setEnabled(false);
            entityManager.flush();

            // Act
            List<User> disabledUsers = userRepository.findByRoleAndEnabled(UserRole.USER, false);
            List<User> enabledAdmins = userRepository.findByRoleAndEnabled(UserRole.ADMIN, true);

            // Assert
            assertThat(disabledUsers).hasSize(1);
            assertThat(enabledAdmins).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Timestamp Auto-Generation")
    class TimestampTests {

        @Test
        @DisplayName("Should set createdAt on persist")
        void shouldSetCreatedAtOnPersist() {
            // Arrange
            User newUser = new User("newuser", "password", "newuser@example.com", UserRole.USER);
            LocalDateTime beforeSave = LocalDateTime.now();

            // Act
            User savedUser = userRepository.save(newUser);
            entityManager.flush();

            // Assert
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getCreatedAt()).isAfterOrEqualTo(beforeSave.minusSeconds(1));
        }

        @Test
        @DisplayName("Should set updatedAt on persist")
        void shouldSetUpdatedAtOnPersist() {
            // Arrange
            User newUser = new User("newuser2", "password", "newuser2@example.com", UserRole.USER);

            // Act
            User savedUser = userRepository.save(newUser);
            entityManager.flush();

            // Assert
            assertThat(savedUser.getUpdatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isEqualTo(savedUser.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Default Role")
    class DefaultRoleTests {

        @Test
        @DisplayName("Should default to USER role when not specified")
        void shouldDefaultToUserRole() {
            // Arrange
            User userWithoutRole = new User();
            userWithoutRole.setUsername("noroleuser");
            userWithoutRole.setPassword("password");
            userWithoutRole.setEmail("norole@example.com");

            // Act
            User savedUser = userRepository.save(userWithoutRole);
            entityManager.flush();

            // Assert
            assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        }
    }
}
