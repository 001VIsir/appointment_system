package org.example.appointment_system.entity;

import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MerchantProfile entity.
 */
class MerchantProfileTest {

    @Test
    void testMerchantProfileCreationWithUserAndBusinessName() {
        // Arrange
        User user = new User("merchant1", "hashedpassword", "merchant@example.com", UserRole.MERCHANT);

        // Act
        MerchantProfile profile = new MerchantProfile(user, "Test Business");

        // Assert
        assertEquals(user, profile.getUser());
        assertEquals("Test Business", profile.getBusinessName());
        assertNull(profile.getDescription());
        assertNull(profile.getPhone());
        assertNull(profile.getAddress());
        assertNull(profile.getSettings());
        assertNull(profile.getId());
        assertNull(profile.getCreatedAt());
        assertNull(profile.getUpdatedAt());
    }

    @Test
    void testMerchantProfileCreationWithFullConstructor() {
        // Arrange
        User user = new User("merchant2", "hashedpassword", "merchant2@example.com", UserRole.MERCHANT);
        String settings = "{\"sessionTimeout\":3600,\"notifications\":true}";

        // Act
        MerchantProfile profile = new MerchantProfile(
            user,
            "Full Business",
            "Business description",
            "123-456-7890",
            "123 Main St",
            settings
        );

        // Assert
        assertEquals(user, profile.getUser());
        assertEquals("Full Business", profile.getBusinessName());
        assertEquals("Business description", profile.getDescription());
        assertEquals("123-456-7890", profile.getPhone());
        assertEquals("123 Main St", profile.getAddress());
        assertEquals(settings, profile.getSettings());
    }

    @Test
    void testMerchantProfileCreationWithNoArgsConstructor() {
        // Arrange & Act
        MerchantProfile profile = new MerchantProfile();

        // Assert
        assertNull(profile.getId());
        assertNull(profile.getUser());
        assertNull(profile.getBusinessName());
        assertNull(profile.getDescription());
        assertNull(profile.getPhone());
        assertNull(profile.getAddress());
        assertNull(profile.getSettings());
        assertNull(profile.getCreatedAt());
        assertNull(profile.getUpdatedAt());
    }

    @Test
    void testPrePersistSetsTimestamps() {
        // Arrange
        User user = new User("merchant3", "hashedpassword", "merchant3@example.com", UserRole.MERCHANT);
        MerchantProfile profile = new MerchantProfile(user, "Test Business");

        // Act
        profile.onCreate();

        // Assert
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
        assertEquals(profile.getCreatedAt(), profile.getUpdatedAt());
    }

    @Test
    void testPreUpdateSetsUpdatedAt() {
        // Arrange
        User user = new User("merchant4", "hashedpassword", "merchant4@example.com", UserRole.MERCHANT);
        MerchantProfile profile = new MerchantProfile(user, "Test Business");
        profile.onCreate();
        LocalDateTime originalCreatedAt = profile.getCreatedAt();

        // Wait a tiny bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        // Act
        profile.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, profile.getCreatedAt()); // createdAt should not change
        assertNotNull(profile.getUpdatedAt());
        assertTrue(profile.getUpdatedAt().isAfter(originalCreatedAt) || profile.getUpdatedAt().equals(originalCreatedAt));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        MerchantProfile profile = new MerchantProfile();
        User user = new User("merchant5", "hashedpassword", "merchant5@example.com", UserRole.MERCHANT);
        String settings = "{\"timezone\":\"Asia/Shanghai\"}";

        // Act
        profile.setId(1L);
        profile.setUser(user);
        profile.setBusinessName("New Business");
        profile.setDescription("New description");
        profile.setPhone("987-654-3210");
        profile.setAddress("456 Oak Ave");
        profile.setSettings(settings);
        profile.setCreatedAt(LocalDateTime.now().minusDays(1));
        profile.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals(1L, profile.getId());
        assertEquals(user, profile.getUser());
        assertEquals("New Business", profile.getBusinessName());
        assertEquals("New description", profile.getDescription());
        assertEquals("987-654-3210", profile.getPhone());
        assertEquals("456 Oak Ave", profile.getAddress());
        assertEquals(settings, profile.getSettings());
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
    }

    @Test
    void testUserAssociation() {
        // Arrange
        User user = new User("merchant6", "hashedpassword", "merchant6@example.com", UserRole.MERCHANT);
        user.setId(100L);

        // Act
        MerchantProfile profile = new MerchantProfile(user, "Associated Business");

        // Assert
        assertNotNull(profile.getUser());
        assertEquals(100L, profile.getUser().getId());
        assertEquals("merchant6", profile.getUser().getUsername());
        assertEquals(UserRole.MERCHANT, profile.getUser().getRole());
    }

    @Test
    void testNullableFieldsCanBeSetAfterCreation() {
        // Arrange
        User user = new User("merchant7", "hashedpassword", "merchant7@example.com", UserRole.MERCHANT);
        MerchantProfile profile = new MerchantProfile(user, "Minimal Business");

        // Initially nullable fields are null
        assertNull(profile.getDescription());
        assertNull(profile.getPhone());
        assertNull(profile.getAddress());
        assertNull(profile.getSettings());

        // Act - set optional fields later
        profile.setDescription("Updated description");
        profile.setPhone("555-1234");
        profile.setAddress("789 Pine St");
        profile.setSettings("{\"key\":\"value\"}");

        // Assert
        assertEquals("Updated description", profile.getDescription());
        assertEquals("555-1234", profile.getPhone());
        assertEquals("789 Pine St", profile.getAddress());
        assertEquals("{\"key\":\"value\"}", profile.getSettings());
    }

    @Test
    void testBusinessNameIsRequired() {
        // This test documents that businessName is required
        // The actual enforcement happens at the database level via @Column(nullable = false)

        // Arrange
        User user = new User("merchant8", "hashedpassword", "merchant8@example.com", UserRole.MERCHANT);

        // Act
        MerchantProfile profile = new MerchantProfile(user, "Required Name");

        // Assert
        assertNotNull(profile.getBusinessName());
        assertEquals("Required Name", profile.getBusinessName());
    }

    @Test
    void testSettingsAsJsonString() {
        // Arrange
        User user = new User("merchant9", "hashedpassword", "merchant9@example.com", UserRole.MERCHANT);
        String complexSettings = "{\"sessionTimeout\":7200,\"notifications\":{\"email\":true,\"sms\":false},\"theme\":\"dark\"}";

        // Act
        MerchantProfile profile = new MerchantProfile(user, "JSON Business");
        profile.setSettings(complexSettings);

        // Assert
        assertEquals(complexSettings, profile.getSettings());
        assertTrue(profile.getSettings().contains("sessionTimeout"));
        assertTrue(profile.getSettings().contains("notifications"));
    }
}
