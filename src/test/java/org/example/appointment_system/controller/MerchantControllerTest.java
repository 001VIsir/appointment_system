package org.example.appointment_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointment_system.dto.request.MerchantProfileRequest;
import org.example.appointment_system.dto.request.MerchantSettingsRequest;
import org.example.appointment_system.dto.response.BookingStatsResponse;
import org.example.appointment_system.dto.response.MerchantProfileResponse;
import org.example.appointment_system.dto.response.MerchantSettingsResponse;
import org.example.appointment_system.service.MerchantService;
import org.example.appointment_system.service.StatisticsService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for MerchantController using standalone MockMvc setup.
 */
@ExtendWith(MockitoExtension.class)
class MerchantControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private MerchantService merchantService;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private MerchantController merchantController;

    private MerchantProfileRequest validProfileRequest;
    private MerchantProfileResponse validProfileResponse;
    private MerchantSettingsRequest validSettingsRequest;
    private MerchantSettingsResponse validSettingsResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(merchantController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        validProfileRequest = MerchantProfileRequest.builder()
            .businessName("Test Business")
            .description("A test business description")
            .phone("+1-555-123-4567")
            .address("123 Test Street")
            .build();

        validProfileResponse = MerchantProfileResponse.builder()
            .id(1L)
            .userId(1L)
            .username("merchantuser")
            .businessName("Test Business")
            .description("A test business description")
            .phone("+1-555-123-4567")
            .address("123 Test Street")
            .settings(null)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        validSettingsRequest = MerchantSettingsRequest.builder()
            .sessionTimeout(14400)
            .notificationsEnabled(true)
            .timezone("Asia/Shanghai")
            .bookingAdvanceDays(30)
            .cancelDeadlineHours(24)
            .autoConfirmBookings(false)
            .maxBookingsPerUserPerDay(5)
            .build();

        validSettingsResponse = MerchantSettingsResponse.builder()
            .sessionTimeout(14400)
            .notificationsEnabled(true)
            .timezone("Asia/Shanghai")
            .bookingAdvanceDays(30)
            .cancelDeadlineHours(24)
            .autoConfirmBookings(false)
            .maxBookingsPerUserPerDay(5)
            .build();
    }

    @Nested
    @DisplayName("POST /api/merchants/profile")
    class CreateProfileEndpointTests {

        @Test
        @DisplayName("should create profile successfully and return 201")
        void createProfile_withValidRequest_shouldReturn201() throws Exception {
            // Given
            when(merchantService.createProfile(any(MerchantProfileRequest.class)))
                .thenReturn(validProfileResponse);

            // When/Then
            mockMvc.perform(post("/api/merchants/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validProfileRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("merchantuser"))
                .andExpect(jsonPath("$.businessName").value("Test Business"))
                .andExpect(jsonPath("$.description").value("A test business description"))
                .andExpect(jsonPath("$.phone").value("+1-555-123-4567"))
                .andExpect(jsonPath("$.address").value("123 Test Street"));

            verify(merchantService).createProfile(any(MerchantProfileRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/profile")
    class GetProfileEndpointTests {

        @Test
        @DisplayName("should return profile when found")
        void getProfile_whenFound_shouldReturn200() throws Exception {
            // Given
            when(merchantService.getCurrentMerchantProfile())
                .thenReturn(Optional.of(validProfileResponse));

            // When/Then
            mockMvc.perform(get("/api/merchants/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.businessName").value("Test Business"))
                .andExpect(jsonPath("$.username").value("merchantuser"));
        }

        @Test
        @DisplayName("should return 404 when profile not found")
        void getProfile_whenNotFound_shouldReturn404() throws Exception {
            // Given
            when(merchantService.getCurrentMerchantProfile())
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/merchants/profile"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/merchants/profile")
    class UpdateProfileEndpointTests {

        @Test
        @DisplayName("should update profile successfully and return 200")
        void updateProfile_withValidRequest_shouldReturn200() throws Exception {
            // Given
            MerchantProfileResponse updatedResponse = MerchantProfileResponse.builder()
                .id(1L)
                .userId(1L)
                .username("merchantuser")
                .businessName("Updated Business")
                .description("Updated description")
                .phone("+1-555-999-9999")
                .address("456 Updated Ave")
                .settings(null)
                .createdAt(validProfileResponse.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

            when(merchantService.updateProfile(any(MerchantProfileRequest.class)))
                .thenReturn(updatedResponse);

            MerchantProfileRequest updateRequest = MerchantProfileRequest.builder()
                .businessName("Updated Business")
                .description("Updated description")
                .phone("+1-555-999-9999")
                .address("456 Updated Ave")
                .build();

            // When/Then
            mockMvc.perform(put("/api/merchants/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Updated Business"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.phone").value("+1-555-999-9999"))
                .andExpect(jsonPath("$.address").value("456 Updated Ave"));

            verify(merchantService).updateProfile(any(MerchantProfileRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/merchants/profile")
    class DeleteProfileEndpointTests {

        @Test
        @DisplayName("should delete profile successfully and return 204")
        void deleteProfile_shouldReturn204() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/merchants/profile"))
                .andExpect(status().isNoContent());

            verify(merchantService).deleteProfile();
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/settings")
    class GetSettingsEndpointTests {

        @Test
        @DisplayName("should return settings when profile exists")
        void getSettings_whenProfileExists_shouldReturn200() throws Exception {
            // Given
            when(merchantService.getSettings()).thenReturn(validSettingsResponse);

            // When/Then
            mockMvc.perform(get("/api/merchants/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionTimeout").value(14400))
                .andExpect(jsonPath("$.notificationsEnabled").value(true))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.bookingAdvanceDays").value(30))
                .andExpect(jsonPath("$.cancelDeadlineHours").value(24))
                .andExpect(jsonPath("$.autoConfirmBookings").value(false))
                .andExpect(jsonPath("$.maxBookingsPerUserPerDay").value(5));
        }

        @Test
        @DisplayName("should return default settings when no settings configured")
        void getSettings_whenNoSettings_shouldReturnDefaults() throws Exception {
            // Given
            MerchantSettingsResponse emptySettings = MerchantSettingsResponse.builder().build();
            when(merchantService.getSettings()).thenReturn(emptySettings);

            // When/Then
            mockMvc.perform(get("/api/merchants/settings"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/merchants/settings")
    class UpdateSettingsEndpointTests {

        @Test
        @DisplayName("should update settings successfully and return 200")
        void updateSettings_withValidRequest_shouldReturn200() throws Exception {
            // Given
            when(merchantService.updateSettings(any(MerchantSettingsRequest.class)))
                .thenReturn(validSettingsResponse);

            // When/Then
            mockMvc.perform(put("/api/merchants/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validSettingsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionTimeout").value(14400))
                .andExpect(jsonPath("$.notificationsEnabled").value(true))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"));

            verify(merchantService).updateSettings(any(MerchantSettingsRequest.class));
        }

        @Test
        @DisplayName("should update partial settings successfully")
        void updateSettings_withPartialRequest_shouldReturn200() throws Exception {
            // Given
            MerchantSettingsRequest partialRequest = MerchantSettingsRequest.builder()
                .notificationsEnabled(false)
                .build();

            MerchantSettingsResponse partialResponse = MerchantSettingsResponse.builder()
                .sessionTimeout(14400)
                .notificationsEnabled(false)
                .timezone("Asia/Shanghai")
                .build();

            when(merchantService.updateSettings(any(MerchantSettingsRequest.class)))
                .thenReturn(partialResponse);

            // When/Then
            mockMvc.perform(put("/api/merchants/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsEnabled").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/profile/exists")
    class HasProfileEndpointTests {

        @Test
        @DisplayName("should return true when profile exists")
        void hasProfile_whenExists_shouldReturnTrue() throws Exception {
            // Given
            when(merchantService.hasMerchantProfile()).thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/merchants/profile/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("should return false when profile does not exist")
        void hasProfile_whenNotExists_shouldReturnFalse() throws Exception {
            // Given
            when(merchantService.hasMerchantProfile()).thenReturn(false);

            // When/Then
            mockMvc.perform(get("/api/merchants/profile/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/stats")
    class GetMerchantStatsEndpointTests {

        @Test
        @DisplayName("should return booking stats for merchant")
        void getMerchantStats_whenProfileExists_shouldReturnStats() throws Exception {
            // Given
            Long merchantId = 1L;
            BookingStatsResponse statsResponse = BookingStatsResponse.builder()
                    .totalBookings(50L)
                    .activeBookings(10L)
                    .pendingBookings(5L)
                    .confirmedBookings(5L)
                    .cancelledBookings(15L)
                    .completedBookings(25L)
                    .todayBookings(3L)
                    .todayActiveBookings(2L)
                    .todayCompletedBookings(1L)
                    .completionRate(50.0)
                    .cancellationRate(30.0)
                    .confirmationRate(10.0)
                    .build();

            when(merchantService.getCurrentMerchantId()).thenReturn(Optional.of(merchantId));
            when(statisticsService.getMerchantBookingStats(merchantId)).thenReturn(statsResponse);

            // When/Then
            mockMvc.perform(get("/api/merchants/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(50))
                .andExpect(jsonPath("$.activeBookings").value(10))
                .andExpect(jsonPath("$.pendingBookings").value(5))
                .andExpect(jsonPath("$.confirmedBookings").value(5))
                .andExpect(jsonPath("$.cancelledBookings").value(15))
                .andExpect(jsonPath("$.completedBookings").value(25))
                .andExpect(jsonPath("$.todayBookings").value(3))
                .andExpect(jsonPath("$.completionRate").value(50.0));

            verify(merchantService).getCurrentMerchantId();
            verify(statisticsService).getMerchantBookingStats(merchantId);
        }

        @Test
        @DisplayName("should throw exception when profile not found")
        void getMerchantStats_whenProfileNotExists_shouldThrowException() throws Exception {
            // Given
            when(merchantService.getCurrentMerchantId()).thenReturn(Optional.empty());

            // When/Then - The exception is thrown because there's no global exception handler
            // In actual application with GlobalExceptionHandler, this would return 404 or 400
            try {
                mockMvc.perform(get("/api/merchants/stats"));
                // If no exception, the test should fail
                org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException to be thrown");
            } catch (Exception e) {
                // Verify that the root cause is IllegalArgumentException
                Throwable rootCause = e;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                org.junit.jupiter.api.Assertions.assertTrue(
                        rootCause instanceof IllegalArgumentException,
                        "Expected IllegalArgumentException but got: " + rootCause.getClass().getName()
                );
                org.junit.jupiter.api.Assertions.assertTrue(
                        rootCause.getMessage().contains("Merchant profile not found"),
                        "Expected message to contain 'Merchant profile not found' but got: " + rootCause.getMessage()
                );
            }

            verify(merchantService).getCurrentMerchantId();
        }
    }
}
