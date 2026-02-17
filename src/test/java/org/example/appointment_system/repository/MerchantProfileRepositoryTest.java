package org.example.appointment_system.repository;

import org.example.appointment_system.entity.MerchantProfile;
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
 * Unit tests for {@link MerchantProfileRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class MerchantProfileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MerchantProfileRepository merchantProfileRepository;

    private User merchantUser1;
    private User merchantUser2;
    private User normalUser;
    private MerchantProfile profile1;
    private MerchantProfile profile2;

    @BeforeEach
    void setUp() {
        // Create test users
        merchantUser1 = new User("merchant1", "password123", "merchant1@example.com", UserRole.MERCHANT);
        merchantUser2 = new User("merchant2", "password456", "merchant2@example.com", UserRole.MERCHANT);
        normalUser = new User("normaluser", "password789", "user@example.com", UserRole.USER);

        // Persist users
        entityManager.persist(merchantUser1);
        entityManager.persist(merchantUser2);
        entityManager.persist(normalUser);
        entityManager.flush();

        // Create merchant profiles
        profile1 = new MerchantProfile(merchantUser1, "Test Business 1",
                "Description 1", "1234567890", "Address 1", "{\"timezone\":\"Asia/Shanghai\"}");
        profile2 = new MerchantProfile(merchantUser2, "Test Business 2",
                "Description 2", "0987654321", "Address 2", null);

        // Persist profiles
        entityManager.persist(profile1);
        entityManager.persist(profile2);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and find merchant profile by ID")
        void shouldSaveAndFindById() {
            // Arrange
            User newUser = new User("newmerchant", "password", "newmerchant@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile(newUser, "New Business");

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(newProfile);
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findById(savedProfile.getId());

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getBusinessName()).isEqualTo("New Business");
            assertThat(foundProfile.get().getUser().getUsername()).isEqualTo("newmerchant");
        }

        @Test
        @DisplayName("Should update merchant profile")
        void shouldUpdateMerchantProfile() {
            // Arrange
            profile1.setBusinessName("Updated Business Name");
            profile1.setPhone("1111111111");

            // Act
            MerchantProfile updatedProfile = merchantProfileRepository.save(profile1);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findById(updatedProfile.getId());
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getBusinessName()).isEqualTo("Updated Business Name");
            assertThat(foundProfile.get().getPhone()).isEqualTo("1111111111");
        }

        @Test
        @DisplayName("Should delete merchant profile")
        void shouldDeleteMerchantProfile() {
            // Act
            merchantProfileRepository.delete(profile1);
            entityManager.flush();

            // Assert
            Optional<MerchantProfile> deletedProfile = merchantProfileRepository.findById(profile1.getId());
            assertThat(deletedProfile).isEmpty();
        }

        @Test
        @DisplayName("Should find all merchant profiles")
        void shouldFindAll() {
            // Act
            List<MerchantProfile> profiles = merchantProfileRepository.findAll();

            // Assert
            assertThat(profiles).hasSize(2);
        }

        @Test
        @DisplayName("Should count merchant profiles")
        void shouldCount() {
            // Act
            long count = merchantProfileRepository.count();

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should save profile with all fields")
        void shouldSaveProfileWithAllFields() {
            // Arrange
            User newUser = new User("fullmerchant", "password", "fullmerchant@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile fullProfile = new MerchantProfile(
                    newUser,
                    "Full Business",
                    "Full description of the business",
                    "+86-10-12345678",
                    "123 Main Street, Beijing",
                    "{\"sessionTimeout\":7200,\"notifications\":true}"
            );

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(fullProfile);
            entityManager.flush();

            // Assert
            assertThat(savedProfile.getId()).isNotNull();
            assertThat(savedProfile.getBusinessName()).isEqualTo("Full Business");
            assertThat(savedProfile.getDescription()).isEqualTo("Full description of the business");
            assertThat(savedProfile.getPhone()).isEqualTo("+86-10-12345678");
            assertThat(savedProfile.getAddress()).isEqualTo("123 Main Street, Beijing");
            assertThat(savedProfile.getSettings()).contains("sessionTimeout");
        }
    }

    @Nested
    @DisplayName("Find by User ID")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find merchant profile by existing user ID")
        void shouldFindByExistingUserId() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUserId(merchantUser1.getId());

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getBusinessName()).isEqualTo("Test Business 1");
        }

        @Test
        @DisplayName("Should return empty for non-existing user ID")
        void shouldReturnEmptyForNonExistingUserId() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUserId(99999L);

            // Assert
            assertThat(foundProfile).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for user without merchant profile")
        void shouldReturnEmptyForUserWithoutProfile() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUserId(normalUser.getId());

            // Assert
            assertThat(foundProfile).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by User Entity")
    class FindByUserTests {

        @Test
        @DisplayName("Should find merchant profile by user entity")
        void shouldFindByUserEntity() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUser(merchantUser1);

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getBusinessName()).isEqualTo("Test Business 1");
        }

        @Test
        @DisplayName("Should return empty for user without profile")
        void shouldReturnEmptyForUserWithoutProfile() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUser(normalUser);

            // Assert
            assertThat(foundProfile).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists by User ID")
    class ExistsByUserIdTests {

        @Test
        @DisplayName("Should return true for existing user ID")
        void shouldReturnTrueForExistingUserId() {
            // Act
            boolean exists = merchantProfileRepository.existsByUserId(merchantUser1.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existing user ID")
        void shouldReturnFalseForNonExistingUserId() {
            // Act
            boolean exists = merchantProfileRepository.existsByUserId(99999L);

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false for user without merchant profile")
        void shouldReturnFalseForUserWithoutProfile() {
            // Act
            boolean exists = merchantProfileRepository.existsByUserId(normalUser.getId());

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Exists by User Entity")
    class ExistsByUserTests {

        @Test
        @DisplayName("Should return true for user with profile")
        void shouldReturnTrueForUserWithProfile() {
            // Act
            boolean exists = merchantProfileRepository.existsByUser(merchantUser1);

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for user without profile")
        void shouldReturnFalseForUserWithoutProfile() {
            // Act
            boolean exists = merchantProfileRepository.existsByUser(normalUser);

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Find by Business Name")
    class FindByBusinessNameTests {

        @Test
        @DisplayName("Should find merchant profile by exact business name")
        void shouldFindByExactBusinessName() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByBusinessName("Test Business 1");

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getUser().getUsername()).isEqualTo("merchant1");
        }

        @Test
        @DisplayName("Should return empty for non-existing business name")
        void shouldReturnEmptyForNonExistingBusinessName() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByBusinessName("Non-existent Business");

            // Assert
            assertThat(foundProfile).isEmpty();
        }

        @Test
        @DisplayName("Should be case-sensitive for business name")
        void shouldBeCaseSensitiveForBusinessName() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByBusinessName("TEST BUSINESS 1");

            // Assert
            assertThat(foundProfile).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists by Business Name")
    class ExistsByBusinessNameTests {

        @Test
        @DisplayName("Should return true for existing business name")
        void shouldReturnTrueForExistingBusinessName() {
            // Act
            boolean exists = merchantProfileRepository.existsByBusinessName("Test Business 1");

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existing business name")
        void shouldReturnFalseForNonExistingBusinessName() {
            // Act
            boolean exists = merchantProfileRepository.existsByBusinessName("Non-existent Business");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should be case-sensitive for business name existence check")
        void shouldBeCaseSensitiveForBusinessNameExistence() {
            // Act
            boolean exists = merchantProfileRepository.existsByBusinessName("test business 1");

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Timestamp Auto-Generation")
    class TimestampTests {

        @Test
        @DisplayName("Should set createdAt on persist")
        void shouldSetCreatedAtOnPersist() {
            // Arrange
            User newUser = new User("timestampuser", "password", "timestamp@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile(newUser, "Timestamp Business");
            LocalDateTime beforeSave = LocalDateTime.now();

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(newProfile);
            entityManager.flush();

            // Assert
            assertThat(savedProfile.getCreatedAt()).isNotNull();
            assertThat(savedProfile.getCreatedAt()).isAfterOrEqualTo(beforeSave.minusSeconds(1));
        }

        @Test
        @DisplayName("Should set updatedAt on persist")
        void shouldSetUpdatedAtOnPersist() {
            // Arrange
            User newUser = new User("timestampuser2", "password", "timestamp2@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile(newUser, "Timestamp Business 2");

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(newProfile);
            entityManager.flush();

            // Assert
            assertThat(savedProfile.getUpdatedAt()).isNotNull();
            assertThat(savedProfile.getUpdatedAt()).isEqualTo(savedProfile.getCreatedAt());
        }

        @Test
        @DisplayName("Should update updatedAt on modification")
        void shouldUpdateUpdatedAtOnModification() throws InterruptedException {
            // Arrange
            LocalDateTime originalUpdatedAt = profile1.getUpdatedAt();

            // Small delay to ensure time difference
            Thread.sleep(10);

            // Act
            profile1.setBusinessName("Modified Business");
            merchantProfileRepository.save(profile1);
            entityManager.flush();

            // Assert
            assertThat(profile1.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("User-Profile Relationship")
    class RelationshipTests {

        @Test
        @DisplayName("Should maintain user-profile relationship")
        void shouldMaintainUserRelationship() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findById(profile1.getId());

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getUser()).isNotNull();
            assertThat(foundProfile.get().getUser().getId()).isEqualTo(merchantUser1.getId());
            assertThat(foundProfile.get().getUser().getUsername()).isEqualTo("merchant1");
            assertThat(foundProfile.get().getUser().getRole()).isEqualTo(UserRole.MERCHANT);
        }

        @Test
        @DisplayName("Should not delete user when profile is deleted")
        void shouldNotDeleteUserWhenProfileDeleted() {
            // Arrange
            Long userId = merchantUser1.getId();

            // Act
            merchantProfileRepository.delete(profile1);
            entityManager.flush();

            // Assert - User should still exist
            User foundUser = entityManager.find(User.class, userId);
            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getUsername()).isEqualTo("merchant1");
        }
    }

    @Nested
    @DisplayName("Settings Field")
    class SettingsTests {

        @Test
        @DisplayName("Should store and retrieve JSON settings")
        void shouldStoreAndRetrieveJsonSettings() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUserId(merchantUser1.getId());

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getSettings()).isNotNull();
            assertThat(foundProfile.get().getSettings()).contains("timezone");
            assertThat(foundProfile.get().getSettings()).contains("Asia/Shanghai");
        }

        @Test
        @DisplayName("Should allow null settings")
        void shouldAllowNullSettings() {
            // Act
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findByUserId(merchantUser2.getId());

            // Assert
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getSettings()).isNull();
        }

        @Test
        @DisplayName("Should update settings")
        void shouldUpdateSettings() {
            // Arrange
            String newSettings = "{\"sessionTimeout\":3600,\"notifications\":false,\"language\":\"zh-CN\"}";
            profile1.setSettings(newSettings);

            // Act
            merchantProfileRepository.save(profile1);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<MerchantProfile> foundProfile = merchantProfileRepository.findById(profile1.getId());
            assertThat(foundProfile).isPresent();
            assertThat(foundProfile.get().getSettings()).isEqualTo(newSettings);
        }
    }

    @Nested
    @DisplayName("Optional Fields")
    class OptionalFieldsTests {

        @Test
        @DisplayName("Should allow null description")
        void shouldAllowNullDescription() {
            // Arrange
            User newUser = new User("nodesc", "password", "nodesc@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile(newUser, "No Description Business");
            newProfile.setDescription(null);

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(newProfile);
            entityManager.flush();

            // Assert
            assertThat(savedProfile.getDescription()).isNull();
        }

        @Test
        @DisplayName("Should allow null phone")
        void shouldAllowNullPhone() {
            // Arrange
            User newUser = new User("nophone", "password", "nophone@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile(newUser, "No Phone Business");
            newProfile.setPhone(null);

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(newProfile);
            entityManager.flush();

            // Assert
            assertThat(savedProfile.getPhone()).isNull();
        }

        @Test
        @DisplayName("Should allow null address")
        void shouldAllowNullAddress() {
            // Arrange
            User newUser = new User("noaddress", "password", "noaddress@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile(newUser, "No Address Business");
            newProfile.setAddress(null);

            // Act
            MerchantProfile savedProfile = merchantProfileRepository.save(newProfile);
            entityManager.flush();

            // Assert
            assertThat(savedProfile.getAddress()).isNull();
        }
    }

    @Nested
    @DisplayName("Business Name Required")
    class BusinessNameRequiredTests {

        @Test
        @DisplayName("Should require business name")
        void shouldRequireBusinessName() {
            // Arrange
            User newUser = new User("noname", "password", "noname@example.com", UserRole.MERCHANT);
            entityManager.persist(newUser);
            entityManager.flush();

            MerchantProfile newProfile = new MerchantProfile();
            newProfile.setUser(newUser);
            // businessName is not set

            // Act & Assert
            // Note: This will throw a constraint violation exception at flush time
            // due to @Column(nullable = false)
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
                merchantProfileRepository.save(newProfile);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }
    }
}
