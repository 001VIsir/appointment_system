package org.example.appointment_system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.appointment_system.dto.request.MerchantProfileRequest;
import org.example.appointment_system.dto.request.MerchantSettingsRequest;
import org.example.appointment_system.dto.response.MerchantProfileResponse;
import org.example.appointment_system.dto.response.MerchantSettingsResponse;
import org.example.appointment_system.entity.MerchantProfile;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MerchantService.
 */
@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MerchantService merchantService;

    private User merchantUser;
    private User regularUser;
    private User adminUser;
    private MerchantProfile merchantProfile;
    private CustomUserDetails merchantUserDetails;
    private CustomUserDetails regularUserDetails;
    private CustomUserDetails adminUserDetails;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        // Create test users
        merchantUser = new User("testmerchant", "encodedPassword", "merchant@test.com", UserRole.MERCHANT);
        merchantUser.setId(1L);

        regularUser = new User("testuser", "encodedPassword", "user@test.com", UserRole.USER);
        regularUser.setId(2L);

        adminUser = new User("testadmin", "encodedPassword", "admin@test.com", UserRole.ADMIN);
        adminUser.setId(3L);

        // Create merchant profile
        merchantProfile = new MerchantProfile(merchantUser, "Test Business");
        merchantProfile.setId(100L);
        merchantProfile.setDescription("Test Description");
        merchantProfile.setPhone("+1234567890");
        merchantProfile.setAddress("123 Test St");

        // Create user details
        merchantUserDetails = new CustomUserDetails(merchantUser);
        regularUserDetails = new CustomUserDetails(regularUser);
        adminUserDetails = new CustomUserDetails(adminUser);
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
    @DisplayName("createProfile tests")
    class CreateProfileTests {

        @Test
        @DisplayName("Should create profile successfully for MERCHANT user")
        void createProfile_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.existsByUserId(1L)).thenReturn(false);
            when(merchantProfileRepository.save(any(MerchantProfile.class))).thenAnswer(invocation -> {
                MerchantProfile profile = invocation.getArgument(0);
                profile.setId(100L);
                return profile;
            });

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("New Business")
                .description("New Description")
                .phone("+0987654321")
                .address("456 New St")
                .build();

            // Act
            MerchantProfileResponse response = merchantService.createProfile(request);

            // Assert
            assertNotNull(response);
            assertEquals("New Business", response.getBusinessName());
            assertEquals("New Description", response.getDescription());
            assertEquals("+0987654321", response.getPhone());
            assertEquals("456 New St", response.getAddress());
            assertEquals(1L, response.getUserId());
            assertEquals("testmerchant", response.getUsername());

            verify(merchantProfileRepository).save(any(MerchantProfile.class));
        }

        @Test
        @DisplayName("Should throw exception when USER role tries to create profile")
        void createProfile_UserRole_ThrowsException() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("New Business")
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.createProfile(request)
            );
            assertTrue(exception.getMessage().contains("MERCHANT role"));
        }

        @Test
        @DisplayName("Should throw exception when ADMIN role tries to create profile")
        void createProfile_AdminRole_ThrowsException() {
            // Arrange
            setSecurityContext(adminUserDetails);
            when(userRepository.findById(3L)).thenReturn(Optional.of(adminUser));

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("New Business")
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.createProfile(request)
            );
            assertTrue(exception.getMessage().contains("MERCHANT role"));
        }

        @Test
        @DisplayName("Should throw exception when profile already exists")
        void createProfile_AlreadyExists_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.existsByUserId(1L)).thenReturn(true);

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("New Business")
                .build();

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> merchantService.createProfile(request)
            );
            assertTrue(exception.getMessage().contains("already has a merchant profile"));
        }

        @Test
        @DisplayName("Should throw exception when no authenticated user")
        void createProfile_NoAuth_ThrowsException() {
            // Arrange
            clearSecurityContext();

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("New Business")
                .build();

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> merchantService.createProfile(request)
            );
            assertTrue(exception.getMessage().contains("No authenticated user"));
        }
    }

    @Nested
    @DisplayName("getCurrentMerchantProfile tests")
    class GetCurrentMerchantProfileTests {

        @Test
        @DisplayName("Should return profile when exists")
        void getCurrentMerchantProfile_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));

            // Act
            Optional<MerchantProfileResponse> response = merchantService.getCurrentMerchantProfile();

            // Assert
            assertTrue(response.isPresent());
            assertEquals("Test Business", response.get().getBusinessName());
            assertEquals(1L, response.get().getUserId());
        }

        @Test
        @DisplayName("Should return empty when profile not found")
        void getCurrentMerchantProfile_NotFound() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // Act
            Optional<MerchantProfileResponse> response = merchantService.getCurrentMerchantProfile();

            // Assert
            assertFalse(response.isPresent());
        }
    }

    @Nested
    @DisplayName("getProfileById tests")
    class GetProfileByIdTests {

        @Test
        @DisplayName("Should return profile when owned by current user")
        void getProfileById_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findById(100L)).thenReturn(Optional.of(merchantProfile));

            // Act
            Optional<MerchantProfileResponse> response = merchantService.getProfileById(100L);

            // Assert
            assertTrue(response.isPresent());
            assertEquals(100L, response.get().getId());
        }

        @Test
        @DisplayName("Should return empty when profile belongs to different user")
        void getProfileById_DifferentUser_ReturnsEmpty() {
            // Arrange - use merchant user but profile belongs to someone else
            User otherMerchant = new User("othermerchant", "pass", "other@test.com", UserRole.MERCHANT);
            otherMerchant.setId(99L);
            MerchantProfile otherProfile = new MerchantProfile(otherMerchant, "Other Business");
            otherProfile.setId(200L);

            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findById(200L)).thenReturn(Optional.of(otherProfile));

            // Act
            Optional<MerchantProfileResponse> response = merchantService.getProfileById(200L);

            // Assert
            assertFalse(response.isPresent());
        }

        @Test
        @DisplayName("Should return empty when profile not found")
        void getProfileById_NotFound() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            Optional<MerchantProfileResponse> response = merchantService.getProfileById(999L);

            // Assert
            assertFalse(response.isPresent());
        }
    }

    @Nested
    @DisplayName("updateProfile tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update profile successfully")
        void updateProfile_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(merchantProfileRepository.save(any(MerchantProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("Updated Business")
                .description("Updated Description")
                .phone("+1111111111")
                .address("999 Updated St")
                .build();

            // Act
            MerchantProfileResponse response = merchantService.updateProfile(request);

            // Assert
            assertEquals("Updated Business", response.getBusinessName());
            assertEquals("Updated Description", response.getDescription());
            assertEquals("+1111111111", response.getPhone());
            assertEquals("999 Updated St", response.getAddress());

            verify(merchantProfileRepository).save(any(MerchantProfile.class));
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void updateProfile_NotFound_ThrowsException() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            MerchantProfileRequest request = MerchantProfileRequest.builder()
                .businessName("Updated Business")
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.updateProfile(request)
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("getSettings tests")
    class GetSettingsTests {

        @Test
        @DisplayName("Should return settings when exists")
        void getSettings_Success() throws JsonProcessingException {
            // Arrange
            String settingsJson = objectMapper.writeValueAsString(Map.of(
                "sessionTimeout", 7200,
                "notificationsEnabled", true,
                "timezone", "Asia/Shanghai"
            ));
            merchantProfile.setSettings(settingsJson);

            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));

            // Act
            MerchantSettingsResponse response = merchantService.getSettings();

            // Assert
            assertEquals(7200, response.getSessionTimeout());
            assertTrue(response.getNotificationsEnabled());
            assertEquals("Asia/Shanghai", response.getTimezone());
        }

        @Test
        @DisplayName("Should return empty settings when null")
        void getSettings_Null() {
            // Arrange
            merchantProfile.setSettings(null);

            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));

            // Act
            MerchantSettingsResponse response = merchantService.getSettings();

            // Assert
            assertNotNull(response);
            assertNull(response.getSessionTimeout());
            assertNull(response.getNotificationsEnabled());
        }

        @Test
        @DisplayName("Should return empty settings when invalid JSON")
        void getSettings_InvalidJson() {
            // Arrange
            merchantProfile.setSettings("invalid json");

            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));

            // Act
            MerchantSettingsResponse response = merchantService.getSettings();

            // Assert
            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("updateSettings tests")
    class UpdateSettingsTests {

        @Test
        @DisplayName("Should update settings successfully")
        void updateSettings_Success() {
            // Arrange
            merchantProfile.setSettings(null);

            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));
            when(merchantProfileRepository.save(any(MerchantProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MerchantSettingsRequest request = MerchantSettingsRequest.builder()
                .sessionTimeout(3600)
                .notificationsEnabled(false)
                .timezone("America/New_York")
                .bookingAdvanceDays(14)
                .cancelDeadlineHours(12)
                .autoConfirmBookings(true)
                .maxBookingsPerUserPerDay(5)
                .build();

            // Act
            MerchantSettingsResponse response = merchantService.updateSettings(request);

            // Assert
            assertEquals(3600, response.getSessionTimeout());
            assertFalse(response.getNotificationsEnabled());
            assertEquals("America/New_York", response.getTimezone());
            assertEquals(14, response.getBookingAdvanceDays());
            assertEquals(12, response.getCancelDeadlineHours());
            assertTrue(response.getAutoConfirmBookings());
            assertEquals(5, response.getMaxBookingsPerUserPerDay());

            verify(merchantProfileRepository).save(any(MerchantProfile.class));
        }

        @Test
        @DisplayName("Should merge with existing settings")
        void updateSettings_Merge() throws JsonProcessingException {
            // Arrange
            String existingSettings = objectMapper.writeValueAsString(Map.of(
                "sessionTimeout", 7200,
                "existingSetting", "value"
            ));
            merchantProfile.setSettings(existingSettings);

            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));

            // Use ArgumentCaptor to verify the saved settings contain merged values
            java.util.concurrent.atomic.AtomicReference<MerchantProfile> savedProfile = new java.util.concurrent.atomic.AtomicReference<>();
            when(merchantProfileRepository.save(any(MerchantProfile.class))).thenAnswer(invocation -> {
                MerchantProfile profile = invocation.getArgument(0);
                savedProfile.set(profile);
                return profile;
            });

            MerchantSettingsRequest request = MerchantSettingsRequest.builder()
                .sessionTimeout(3600) // This should override the existing value
                .notificationsEnabled(true) // This is new
                .build();

            // Act
            MerchantSettingsResponse response = merchantService.updateSettings(request);

            // Assert
            assertNotNull(savedProfile.get());
            String savedSettingsJson = savedProfile.get().getSettings();
            assertNotNull(savedSettingsJson);
            // Verify the JSON contains the updated sessionTimeout
            assertTrue(savedSettingsJson.contains("3600"));
            assertTrue(savedSettingsJson.contains("notificationsEnabled"));
            // Verify existingSetting is preserved
            assertTrue(savedSettingsJson.contains("existingSetting"));
            assertTrue(savedSettingsJson.contains("value"));
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void updateSettings_ProfileNotFound() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            MerchantSettingsRequest request = MerchantSettingsRequest.builder()
                .sessionTimeout(3600)
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.updateSettings(request)
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("deleteProfile tests")
    class DeleteProfileTests {

        @Test
        @DisplayName("Should delete profile successfully")
        void deleteProfile_Success() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(merchantProfile));

            // Act
            merchantService.deleteProfile();

            // Assert
            verify(merchantProfileRepository).delete(merchantProfile);
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void deleteProfile_NotFound() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.deleteProfile()
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("hasMerchantProfile tests")
    class HasMerchantProfileTests {

        @Test
        @DisplayName("Should return true when profile exists")
        void hasMerchantProfile_True() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.existsByUserId(1L)).thenReturn(true);

            // Act
            boolean result = merchantService.hasMerchantProfile();

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when profile not exists")
        void hasMerchantProfile_False() {
            // Arrange
            setSecurityContext(merchantUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(merchantProfileRepository.existsByUserId(1L)).thenReturn(false);

            // Act
            boolean result = merchantService.hasMerchantProfile();

            // Assert
            assertFalse(result);
        }
    }
}
