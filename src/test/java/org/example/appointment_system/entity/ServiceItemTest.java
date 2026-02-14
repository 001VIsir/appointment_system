package org.example.appointment_system.entity;

import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ServiceItem entity.
 */
class ServiceItemTest {

    @Test
    void testServiceItemCreationWithRequiredFields() {
        // Arrange
        User user = new User("merchant1", "hashedpassword", "merchant@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Test Business");

        // Act
        ServiceItem serviceItem = new ServiceItem(
            merchant,
            "Haircut",
            ServiceCategory.BEAUTY,
            30,
            new BigDecimal("25.00")
        );

        // Assert
        assertEquals(merchant, serviceItem.getMerchant());
        assertEquals("Haircut", serviceItem.getName());
        assertEquals(ServiceCategory.BEAUTY, serviceItem.getCategory());
        assertEquals(30, serviceItem.getDuration());
        assertEquals(new BigDecimal("25.00"), serviceItem.getPrice());
        assertNull(serviceItem.getDescription());
        assertNull(serviceItem.getId());
        assertNull(serviceItem.getCreatedAt());
        assertNull(serviceItem.getUpdatedAt());
    }

    @Test
    void testServiceItemCreationWithFullConstructor() {
        // Arrange
        User user = new User("merchant2", "hashedpassword", "merchant2@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Full Business");

        // Act
        ServiceItem serviceItem = new ServiceItem(
            merchant,
            "Consultation",
            "Professional consultation service",
            ServiceCategory.CONSULTATION,
            60,
            new BigDecimal("100.00"),
            true
        );

        // Assert
        assertEquals(merchant, serviceItem.getMerchant());
        assertEquals("Consultation", serviceItem.getName());
        assertEquals("Professional consultation service", serviceItem.getDescription());
        assertEquals(ServiceCategory.CONSULTATION, serviceItem.getCategory());
        assertEquals(60, serviceItem.getDuration());
        assertEquals(new BigDecimal("100.00"), serviceItem.getPrice());
        assertTrue(serviceItem.getActive());
    }

    @Test
    void testServiceItemCreationWithNoArgsConstructor() {
        // Arrange & Act
        ServiceItem serviceItem = new ServiceItem();

        // Assert
        assertNull(serviceItem.getId());
        assertNull(serviceItem.getMerchant());
        assertNull(serviceItem.getName());
        assertNull(serviceItem.getDescription());
        assertNull(serviceItem.getCreatedAt());
        assertNull(serviceItem.getUpdatedAt());
        // Check defaults
        assertEquals(ServiceCategory.GENERAL, serviceItem.getCategory());
        assertEquals(30, serviceItem.getDuration());
        assertEquals(BigDecimal.ZERO, serviceItem.getPrice());
        assertTrue(serviceItem.getActive());
    }

    @Test
    void testDefaultValues() {
        // Arrange & Act
        ServiceItem serviceItem = new ServiceItem();

        // Assert - check default values
        assertEquals(ServiceCategory.GENERAL, serviceItem.getCategory());
        assertEquals(30, serviceItem.getDuration());
        assertEquals(BigDecimal.ZERO, serviceItem.getPrice());
        assertTrue(serviceItem.getActive());
    }

    @Test
    void testPrePersistSetsTimestamps() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();

        // Act
        serviceItem.onCreate();

        // Assert
        assertNotNull(serviceItem.getCreatedAt());
        assertNotNull(serviceItem.getUpdatedAt());
        assertEquals(serviceItem.getCreatedAt(), serviceItem.getUpdatedAt());
    }

    @Test
    void testPreUpdateSetsUpdatedAt() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.onCreate();
        LocalDateTime originalCreatedAt = serviceItem.getCreatedAt();

        // Wait a tiny bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        // Act
        serviceItem.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, serviceItem.getCreatedAt()); // createdAt should not change
        assertNotNull(serviceItem.getUpdatedAt());
        assertTrue(serviceItem.getUpdatedAt().isAfter(originalCreatedAt) || serviceItem.getUpdatedAt().equals(originalCreatedAt));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();
        User user = new User("merchant3", "hashedpassword", "merchant3@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Setter Business");

        // Act
        serviceItem.setId(1L);
        serviceItem.setMerchant(merchant);
        serviceItem.setName("Test Service");
        serviceItem.setDescription("Test Description");
        serviceItem.setCategory(ServiceCategory.MEDICAL);
        serviceItem.setDuration(45);
        serviceItem.setPrice(new BigDecimal("50.00"));
        serviceItem.setActive(false);
        serviceItem.setCreatedAt(LocalDateTime.now().minusDays(1));
        serviceItem.setUpdatedAt(LocalDateTime.now());

        // Assert
        assertEquals(1L, serviceItem.getId());
        assertEquals(merchant, serviceItem.getMerchant());
        assertEquals("Test Service", serviceItem.getName());
        assertEquals("Test Description", serviceItem.getDescription());
        assertEquals(ServiceCategory.MEDICAL, serviceItem.getCategory());
        assertEquals(45, serviceItem.getDuration());
        assertEquals(new BigDecimal("50.00"), serviceItem.getPrice());
        assertFalse(serviceItem.getActive());
        assertNotNull(serviceItem.getCreatedAt());
        assertNotNull(serviceItem.getUpdatedAt());
    }

    @Test
    void testMerchantAssociation() {
        // Arrange
        User user = new User("merchant4", "hashedpassword", "merchant4@example.com", UserRole.MERCHANT);
        user.setId(100L);
        MerchantProfile merchant = new MerchantProfile(user, "Associated Business");
        merchant.setId(200L);

        // Act
        ServiceItem serviceItem = new ServiceItem(merchant, "Service", ServiceCategory.GENERAL, 30, BigDecimal.ZERO);

        // Assert
        assertNotNull(serviceItem.getMerchant());
        assertEquals(200L, serviceItem.getMerchant().getId());
        assertEquals("Associated Business", serviceItem.getMerchant().getBusinessName());
        assertEquals(user, serviceItem.getMerchant().getUser());
    }

    @Test
    void testAllServiceCategories() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();
        ServiceCategory[] categories = ServiceCategory.values();

        // Act & Assert - test each category
        for (ServiceCategory category : categories) {
            serviceItem.setCategory(category);
            assertEquals(category, serviceItem.getCategory());
        }

        // Verify we have expected categories
        assertEquals(7, categories.length);
    }

    @Test
    void testPricePrecision() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();
        BigDecimal highPrecisionPrice = new BigDecimal("12345678.99");

        // Act
        serviceItem.setPrice(highPrecisionPrice);

        // Assert
        assertEquals(highPrecisionPrice, serviceItem.getPrice());
        assertEquals(2, serviceItem.getPrice().scale());
    }

    @Test
    void testActiveToggle() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();
        assertTrue(serviceItem.getActive()); // Default is true

        // Act
        serviceItem.setActive(false);

        // Assert
        assertFalse(serviceItem.getActive());

        // Act again
        serviceItem.setActive(true);

        // Assert
        assertTrue(serviceItem.getActive());
    }

    @Test
    void testDurationInMinutes() {
        // Arrange
        ServiceItem serviceItem = new ServiceItem();

        // Act & Assert - test various durations
        serviceItem.setDuration(15);
        assertEquals(15, serviceItem.getDuration());

        serviceItem.setDuration(60);
        assertEquals(60, serviceItem.getDuration());

        serviceItem.setDuration(120);
        assertEquals(120, serviceItem.getDuration());
    }

    @Test
    void testNullableFieldsCanBeSetAfterCreation() {
        // Arrange
        User user = new User("merchant5", "hashedpassword", "merchant5@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Business");
        ServiceItem serviceItem = new ServiceItem(merchant, "Minimal Service", ServiceCategory.GENERAL, 30, BigDecimal.ZERO);

        // Initially description is null
        assertNull(serviceItem.getDescription());

        // Act - set optional fields later
        serviceItem.setDescription("Updated description");

        // Assert
        assertEquals("Updated description", serviceItem.getDescription());
    }

    @Test
    void testServiceNameIsRequired() {
        // This test documents that name is required
        // The actual enforcement happens at the database level via @Column(nullable = false)

        // Arrange
        User user = new User("merchant6", "hashedpassword", "merchant6@example.com", UserRole.MERCHANT);
        MerchantProfile merchant = new MerchantProfile(user, "Business");

        // Act
        ServiceItem serviceItem = new ServiceItem(merchant, "Required Name", ServiceCategory.GENERAL, 30, BigDecimal.ZERO);

        // Assert
        assertNotNull(serviceItem.getName());
        assertEquals("Required Name", serviceItem.getName());
    }
}
