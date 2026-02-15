package org.example.appointment_system.service;

import org.example.appointment_system.dto.request.ServiceItemRequest;
import org.example.appointment_system.dto.response.ServiceItemResponse;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.repository.UserRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceItemService.
 */
@ExtendWith(MockitoExtension.class)
class ServiceItemServiceTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ServiceItemService serviceItemService;

    private User merchantUser;
    private MerchantProfile merchantProfile;
    private CustomUserDetails merchantUserDetails;
    private ServiceItem serviceItem1;
    private ServiceItem serviceItem2;

    @BeforeEach
    void setUp() {
        // Create test user
        merchantUser = new User("testmerchant", "encodedPassword", "merchant@test.com", UserRole.MERCHANT);
        merchantUser.setId(1L);

        // Create merchant profile
        merchantProfile = new MerchantProfile(merchantUser, "Test Business");
        merchantProfile.setId(100L);
        merchantProfile.setDescription("Test Description");

        // Create user details
        merchantUserDetails = new CustomUserDetails(merchantUser);

        // Create test service items
        serviceItem1 = new ServiceItem(
            merchantProfile,
            "Haircut",
            "Professional haircut service",
            ServiceCategory.BEAUTY,
            30,
            new BigDecimal("25.00"),
            true
        );
        serviceItem1.setId(1L);

        serviceItem2 = new ServiceItem(
            merchantProfile,
            "Massage",
            "Full body massage",
            ServiceCategory.BEAUTY,
            60,
            new BigDecimal("50.00"),
            true
        );
        serviceItem2.setId(2L);
    }

    private void setSecurityContext(CustomUserDetails userDetails) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createServiceItem tests")
    class CreateServiceItemTests {

        @Test
        @DisplayName("Should create service item successfully")
        void createServiceItem_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.existsByMerchantAndName(merchantProfile, "New Service")).thenReturn(false);
            when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> {
                ServiceItem item = invocation.getArgument(0);
                item.setId(10L);
                return item;
            });

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("New Service")
                .description("New Description")
                .category(ServiceCategory.MEDICAL)
                .duration(45)
                .price(new BigDecimal("75.00"))
                .build();

            // Act
            ServiceItemResponse response = serviceItemService.createServiceItem(request);

            // Assert
            assertNotNull(response);
            assertEquals("New Service", response.getName());
            assertEquals("New Description", response.getDescription());
            assertEquals(ServiceCategory.MEDICAL, response.getCategory());
            assertEquals(45, response.getDuration());
            assertEquals(new BigDecimal("75.00"), response.getPrice());
            assertTrue(response.getActive()); // Default value
            assertEquals(100L, response.getMerchantId());
            assertEquals("Test Business", response.getMerchantBusinessName());

            verify(serviceItemRepository).save(any(ServiceItem.class));
        }

        @Test
        @DisplayName("Should create service item with active=false when specified")
        void createServiceItem_Inactive() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.existsByMerchantAndName(merchantProfile, "Inactive Service")).thenReturn(false);
            when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> {
                ServiceItem item = invocation.getArgument(0);
                item.setId(11L);
                return item;
            });

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Inactive Service")
                .category(ServiceCategory.GENERAL)
                .duration(15)
                .price(BigDecimal.ZERO)
                .active(false)
                .build();

            // Act
            ServiceItemResponse response = serviceItemService.createServiceItem(request);

            // Assert
            assertFalse(response.getActive());
        }

        @Test
        @DisplayName("Should throw exception when service name already exists")
        void createServiceItem_DuplicateName_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.existsByMerchantAndName(merchantProfile, "Haircut")).thenReturn(true);

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Haircut")
                .category(ServiceCategory.BEAUTY)
                .duration(30)
                .price(new BigDecimal("25.00"))
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceItemService.createServiceItem(request)
            );
            assertTrue(exception.getMessage().contains("already exists"));
            verify(serviceItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when no merchant profile")
        void createServiceItem_NoMerchantProfile_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Test Service")
                .category(ServiceCategory.GENERAL)
                .duration(30)
                .price(BigDecimal.TEN)
                .build();

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> serviceItemService.createServiceItem(request)
            );
            assertTrue(exception.getMessage().contains("No merchant profile"));
        }

        @Test
        @DisplayName("Should throw exception when no authentication")
        void createServiceItem_NoAuth_ThrowsException() {
            // Arrange
            clearSecurityContext();

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Test Service")
                .category(ServiceCategory.GENERAL)
                .duration(30)
                .price(BigDecimal.TEN)
                .build();

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> serviceItemService.createServiceItem(request)
            );
            assertTrue(exception.getMessage().contains("No merchant profile"));
        }
    }

    @Nested
    @DisplayName("updateServiceItem tests")
    class UpdateServiceItemTests {

        @Test
        @DisplayName("Should update service item successfully")
        void updateServiceItem_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(1L, merchantProfile)).thenReturn(Optional.of(serviceItem1));
            when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Updated Haircut")
                .description("Updated description")
                .category(ServiceCategory.BEAUTY)
                .duration(45)
                .price(new BigDecimal("30.00"))
                .active(true)
                .build();

            // Act
            ServiceItemResponse response = serviceItemService.updateServiceItem(1L, request);

            // Assert
            assertEquals("Updated Haircut", response.getName());
            assertEquals("Updated description", response.getDescription());
            assertEquals(45, response.getDuration());
            assertEquals(new BigDecimal("30.00"), response.getPrice());

            verify(serviceItemRepository).save(any(ServiceItem.class));
        }

        @Test
        @DisplayName("Should throw exception when new name already exists")
        void updateServiceItem_DuplicateNewName_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(1L, merchantProfile)).thenReturn(Optional.of(serviceItem1));
            when(serviceItemRepository.existsByMerchantAndName(merchantProfile, "Massage")).thenReturn(true);

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Massage") // Trying to rename to existing name
                .category(ServiceCategory.BEAUTY)
                .duration(60)
                .price(new BigDecimal("50.00"))
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceItemService.updateServiceItem(1L, request)
            );
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Should allow update without changing name")
        void updateServiceItem_SameName_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(1L, merchantProfile)).thenReturn(Optional.of(serviceItem1));
            when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Haircut") // Same name as existing
                .description("Updated description")
                .category(ServiceCategory.BEAUTY)
                .duration(40)
                .price(new BigDecimal("28.00"))
                .build();

            // Act
            ServiceItemResponse response = serviceItemService.updateServiceItem(1L, request);

            // Assert
            assertEquals("Haircut", response.getName());
            assertEquals(40, response.getDuration());
            // Should not check for duplicate since name is the same
            verify(serviceItemRepository, never()).existsByMerchantAndName(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when service not found")
        void updateServiceItem_NotFound_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(999L, merchantProfile)).thenReturn(Optional.empty());

            ServiceItemRequest request = ServiceItemRequest.builder()
                .name("Test")
                .category(ServiceCategory.GENERAL)
                .duration(30)
                .price(BigDecimal.TEN)
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceItemService.updateServiceItem(999L, request)
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("deleteServiceItem tests")
    class DeleteServiceItemTests {

        @Test
        @DisplayName("Should soft delete service item successfully")
        void deleteServiceItem_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(1L, merchantProfile)).thenReturn(Optional.of(serviceItem1));
            when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            serviceItemService.deleteServiceItem(1L);

            // Assert
            verify(serviceItemRepository).save(argThat(item -> !item.getActive()));
        }

        @Test
        @DisplayName("Should throw exception when service not found")
        void deleteServiceItem_NotFound_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(999L, merchantProfile)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceItemService.deleteServiceItem(999L)
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("reactivateServiceItem tests")
    class ReactivateServiceItemTests {

        @Test
        @DisplayName("Should reactivate service item successfully")
        void reactivateServiceItem_Success() {
            // Arrange
            serviceItem1.setActive(false); // Already soft-deleted

            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(1L, merchantProfile)).thenReturn(Optional.of(serviceItem1));
            when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ServiceItemResponse response = serviceItemService.reactivateServiceItem(1L);

            // Assert
            assertTrue(response.getActive());
            verify(serviceItemRepository).save(argThat(ServiceItem::getActive));
        }
    }

    @Nested
    @DisplayName("getServiceItemById tests")
    class GetServiceItemByIdTests {

        @Test
        @DisplayName("Should return service item when found")
        void getServiceItemById_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(1L, merchantProfile)).thenReturn(Optional.of(serviceItem1));

            // Act
            Optional<ServiceItemResponse> response = serviceItemService.getServiceItemById(1L);

            // Assert
            assertTrue(response.isPresent());
            assertEquals("Haircut", response.get().getName());
            assertEquals(ServiceCategory.BEAUTY, response.get().getCategory());
        }

        @Test
        @DisplayName("Should return empty when not found")
        void getServiceItemById_NotFound() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByIdAndMerchant(999L, merchantProfile)).thenReturn(Optional.empty());

            // Act
            Optional<ServiceItemResponse> response = serviceItemService.getServiceItemById(999L);

            // Assert
            assertFalse(response.isPresent());
        }
    }

    @Nested
    @DisplayName("getAllServiceItems tests")
    class GetAllServiceItemsTests {

        @Test
        @DisplayName("Should return all service items for merchant")
        void getAllServiceItems_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByMerchant(merchantProfile)).thenReturn(List.of(serviceItem1, serviceItem2));

            // Act
            List<ServiceItemResponse> response = serviceItemService.getAllServiceItems();

            // Assert
            assertEquals(2, response.size());
            assertEquals("Haircut", response.get(0).getName());
            assertEquals("Massage", response.get(1).getName());
        }

        @Test
        @DisplayName("Should return empty list when no services")
        void getAllServiceItems_Empty() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByMerchant(merchantProfile)).thenReturn(List.of());

            // Act
            List<ServiceItemResponse> response = serviceItemService.getAllServiceItems();

            // Assert
            assertTrue(response.isEmpty());
        }
    }

    @Nested
    @DisplayName("getActiveServiceItems tests")
    class GetActiveServiceItemsTests {

        @Test
        @DisplayName("Should return only active service items")
        void getActiveServiceItems_Success() {
            // Arrange
            ServiceItem inactiveItem = new ServiceItem(
                merchantProfile, "Inactive", "desc", ServiceCategory.GENERAL, 10, BigDecimal.ONE, false
            );
            inactiveItem.setId(3L);

            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByMerchantAndActiveTrue(merchantProfile)).thenReturn(List.of(serviceItem1, serviceItem2));

            // Act
            List<ServiceItemResponse> response = serviceItemService.getActiveServiceItems();

            // Assert
            assertEquals(2, response.size());
            assertTrue(response.stream().allMatch(ServiceItemResponse::getActive));
        }
    }

    @Nested
    @DisplayName("getInactiveServiceItems tests")
    class GetInactiveServiceItemsTests {

        @Test
        @DisplayName("Should return only inactive service items")
        void getInactiveServiceItems_Success() {
            // Arrange
            ServiceItem inactiveItem = new ServiceItem(
                merchantProfile, "Inactive", "desc", ServiceCategory.GENERAL, 10, BigDecimal.ONE, false
            );
            inactiveItem.setId(3L);

            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.findByMerchantAndActiveFalse(merchantProfile)).thenReturn(List.of(inactiveItem));

            // Act
            List<ServiceItemResponse> response = serviceItemService.getInactiveServiceItems();

            // Assert
            assertEquals(1, response.size());
            assertFalse(response.get(0).getActive());
            assertEquals("Inactive", response.get(0).getName());
        }
    }

    @Nested
    @DisplayName("countAllServiceItems tests")
    class CountAllServiceItemsTests {

        @Test
        @DisplayName("Should return total count")
        void countAllServiceItems_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.countByMerchant(merchantProfile)).thenReturn(5L);

            // Act
            long count = serviceItemService.countAllServiceItems();

            // Assert
            assertEquals(5L, count);
        }
    }

    @Nested
    @DisplayName("countActiveServiceItems tests")
    class CountActiveServiceItemsTests {

        @Test
        @DisplayName("Should return active count")
        void countActiveServiceItems_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.countByMerchantAndActiveTrue(merchantProfile)).thenReturn(3L);

            // Act
            long count = serviceItemService.countActiveServiceItems();

            // Assert
            assertEquals(3L, count);
        }
    }

    @Nested
    @DisplayName("existsByName tests")
    class ExistsByNameTests {

        @Test
        @DisplayName("Should return true when exists")
        void existsByName_True() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.existsByMerchantAndName(merchantProfile, "Haircut")).thenReturn(true);

            // Act
            boolean exists = serviceItemService.existsByName("Haircut");

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Should return false when not exists")
        void existsByName_False() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(serviceItemRepository.existsByMerchantAndName(merchantProfile, "NonExistent")).thenReturn(false);

            // Act
            boolean exists = serviceItemService.existsByName("NonExistent");

            // Assert
            assertFalse(exists);
        }
    }
}
