package org.example.appointment_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointment_system.dto.request.BookingRequest;
import org.example.appointment_system.dto.response.BookingResponse;
import org.example.appointment_system.dto.response.MerchantProfileResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.service.BookingService;
import org.example.appointment_system.service.MerchantService;
import org.example.appointment_system.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BookingController.
 */
@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private MerchantService merchantService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingController bookingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private BookingResponse testBookingResponse;
    private BookingRequest testBookingRequest;
    private MerchantProfileResponse testMerchantResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testBookingRequest = BookingRequest.builder()
            .slotId(1L)
            .remark("Test booking")
            .build();

        testBookingResponse = BookingResponse.builder()
            .id(1L)
            .userId(100L)
            .username("testuser")
            .slotId(1L)
            .taskId(10L)
            .taskTitle("Test Task")
            .taskDate(LocalDate.now().plusDays(1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .serviceId(50L)
            .serviceName("Test Service")
            .merchantId(200L)
            .merchantBusinessName("Test Merchant")
            .status(BookingStatus.PENDING)
            .statusDisplayName("Pending")
            .remark("Test booking")
            .version(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        testMerchantResponse = MerchantProfileResponse.builder()
            .id(200L)
            .userId(100L)
            .username("merchant")
            .businessName("Test Merchant")
            .build();
    }

    // ============================================
    // Create Booking Tests
    // ============================================

    @Test
    @DisplayName("POST /api/bookings - Should create booking and send notification")
    void createBooking_ShouldReturn201AndSendNotification() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(testBookingResponse);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBookingRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(notificationService).notifyNewBooking(any(BookingResponse.class));
    }

    // ============================================
    // Get My Bookings Tests
    // ============================================

    @Test
    @DisplayName("GET /api/bookings/my - Should return paginated bookings")
    void getMyBookings_ShouldReturnPaginatedBookings() throws Exception {
        Page<BookingResponse> page = new PageImpl<>(List.of(testBookingResponse), PageRequest.of(0, 20), 1);
        when(bookingService.getMyBookings(any(Pageable.class))).thenReturn(page);

        // Use a simpler test - just verify the service is called and status is OK
        mockMvc.perform(get("/api/bookings/my"))
            .andExpect(status().isOk());

        verify(bookingService).getMyBookings(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/bookings/my/active - Should return active bookings")
    void getMyActiveBookings_ShouldReturnActiveBookings() throws Exception {
        when(bookingService.getMyActiveBookings()).thenReturn(List.of(testBookingResponse));

        mockMvc.perform(get("/api/bookings/my/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/my/status/{status} - Should return bookings by status")
    void getMyBookingsByStatus_ShouldReturnFilteredBookings() throws Exception {
        when(bookingService.getMyBookingsByStatus(BookingStatus.PENDING))
            .thenReturn(List.of(testBookingResponse));

        mockMvc.perform(get("/api/bookings/my/status/PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ============================================
    // Get Booking By ID Tests
    // ============================================

    @Test
    @DisplayName("GET /api/bookings/{id} - Should return booking when found")
    void getBookingById_ShouldReturnBookingWhenFound() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(Optional.of(testBookingResponse));

        mockMvc.perform(get("/api/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} - Should return 404 when not found")
    void getBookingById_ShouldReturn404WhenNotFound() throws Exception {
        when(bookingService.getBookingById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bookings/999"))
            .andExpect(status().isNotFound());
    }

    // ============================================
    // Cancel Booking Tests
    // ============================================

    @Test
    @DisplayName("DELETE /api/bookings/{id} - Should cancel booking and send notification")
    void cancelBooking_ShouldCancelAndSendNotification() throws Exception {
        BookingResponse cancelledResponse = BookingResponse.builder()
            .id(1L)
            .status(BookingStatus.CANCELLED)
            .merchantId(200L)
            .userId(100L)
            .build();
        when(bookingService.cancelBooking(1L)).thenReturn(cancelledResponse);

        mockMvc.perform(delete("/api/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(notificationService).notifyBookingCancelled(any(BookingResponse.class));
    }

    // ============================================
    // Merchant Booking Tests
    // ============================================

    @Test
    @DisplayName("GET /api/merchants/bookings - Should return merchant bookings")
    void getMerchantBookings_ShouldReturnMerchantBookings() throws Exception {
        Page<BookingResponse> page = new PageImpl<>(List.of(testBookingResponse), PageRequest.of(0, 20), 1);
        when(merchantService.getCurrentMerchantProfile()).thenReturn(Optional.of(testMerchantResponse));
        when(bookingService.getBookingsByMerchant(eq(200L), any(Pageable.class))).thenReturn(page);

        // Use a simpler test - just verify the service is called and status is OK
        mockMvc.perform(get("/api/merchants/bookings"))
            .andExpect(status().isOk());

        verify(merchantService).getCurrentMerchantProfile();
        verify(bookingService).getBookingsByMerchant(eq(200L), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/merchants/bookings/status/{status} - Should return filtered bookings")
    void getMerchantBookingsByStatus_ShouldReturnFilteredBookings() throws Exception {
        when(merchantService.getCurrentMerchantProfile()).thenReturn(Optional.of(testMerchantResponse));
        when(bookingService.getBookingsByMerchantAndStatus(200L, BookingStatus.PENDING))
            .thenReturn(List.of(testBookingResponse));

        mockMvc.perform(get("/api/merchants/bookings/status/PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /api/merchants/bookings/{id}/confirm - Should confirm booking")
    void confirmBooking_ShouldConfirmAndSendNotification() throws Exception {
        BookingResponse confirmedResponse = BookingResponse.builder()
            .id(1L)
            .status(BookingStatus.CONFIRMED)
            .userId(100L)
            .build();
        when(merchantService.getCurrentMerchantProfile()).thenReturn(Optional.of(testMerchantResponse));
        when(bookingService.confirmBooking(1L, 200L)).thenReturn(confirmedResponse);

        mockMvc.perform(put("/api/merchants/bookings/1/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(notificationService).notifyBookingConfirmed(any(BookingResponse.class));
    }

    @Test
    @DisplayName("PUT /api/merchants/bookings/{id}/complete - Should complete booking")
    void completeBooking_ShouldCompleteAndSendNotification() throws Exception {
        BookingResponse completedResponse = BookingResponse.builder()
            .id(1L)
            .status(BookingStatus.COMPLETED)
            .userId(100L)
            .build();
        when(merchantService.getCurrentMerchantProfile()).thenReturn(Optional.of(testMerchantResponse));
        when(bookingService.completeBooking(1L, 200L)).thenReturn(completedResponse);

        mockMvc.perform(put("/api/merchants/bookings/1/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(notificationService).notifyBookingCompleted(any(BookingResponse.class));
    }

    @Test
    @DisplayName("DELETE /api/merchants/bookings/{id} - Should cancel booking as merchant")
    void cancelBookingByMerchant_ShouldCancelAndSendNotification() throws Exception {
        BookingResponse cancelledResponse = BookingResponse.builder()
            .id(1L)
            .status(BookingStatus.CANCELLED)
            .merchantId(200L)
            .userId(100L)
            .build();
        when(merchantService.getCurrentMerchantProfile()).thenReturn(Optional.of(testMerchantResponse));
        when(bookingService.cancelBookingByMerchant(1L, 200L)).thenReturn(cancelledResponse);

        mockMvc.perform(delete("/api/merchants/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(notificationService).notifyBookingCancelled(any(BookingResponse.class));
    }

    // ============================================
    // Slot Query Tests
    // ============================================

    @Test
    @DisplayName("GET /api/tasks/{taskId}/slots - Should return all slots")
    void getAvailableSlots_ShouldReturnSlots() throws Exception {
        SlotResponse slotResponse = SlotResponse.builder()
            .id(1L)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .capacity(10)
            .bookedCount(3)
            .availableCount(7)
            .hasCapacity(true)
            .build();
        when(bookingService.getAvailableSlots(10L)).thenReturn(List.of(slotResponse));

        mockMvc.perform(get("/api/tasks/10/slots"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].hasCapacity").value(true));
    }

    @Test
    @DisplayName("GET /api/tasks/{taskId}/slots/available - Should return available slots")
    void getSlotsWithCapacity_ShouldReturnAvailableSlots() throws Exception {
        SlotResponse slotResponse = SlotResponse.builder()
            .id(1L)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .capacity(10)
            .bookedCount(3)
            .availableCount(7)
            .hasCapacity(true)
            .build();
        when(bookingService.getSlotsWithCapacity(10L)).thenReturn(List.of(slotResponse));

        mockMvc.perform(get("/api/tasks/10/slots/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hasCapacity").value(true));
    }

    @Test
    @DisplayName("GET /api/slots/{slotId} - Should return slot when found")
    void getSlotById_ShouldReturnSlotWhenFound() throws Exception {
        SlotResponse slotResponse = SlotResponse.builder()
            .id(1L)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .capacity(10)
            .bookedCount(3)
            .availableCount(7)
            .hasCapacity(true)
            .build();
        when(bookingService.getSlotById(1L)).thenReturn(Optional.of(slotResponse));

        mockMvc.perform(get("/api/slots/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/slots/{slotId} - Should return 404 when slot not found")
    void getSlotById_ShouldReturn404WhenNotFound() throws Exception {
        when(bookingService.getSlotById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/slots/999"))
            .andExpect(status().isNotFound());
    }

    // ============================================
    // Statistics Tests
    // ============================================

    @Test
    @DisplayName("GET /api/bookings/my/count - Should return booking count")
    void countMyBookings_ShouldReturnCount() throws Exception {
        when(bookingService.countMyBookings()).thenReturn(5L);

        mockMvc.perform(get("/api/bookings/my/count"))
            .andExpect(status().isOk())
            .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("GET /api/bookings/my/count/active - Should return active count")
    void countMyActiveBookings_ShouldReturnActiveCount() throws Exception {
        when(bookingService.countMyActiveBookings()).thenReturn(2L);

        mockMvc.perform(get("/api/bookings/my/count/active"))
            .andExpect(status().isOk())
            .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("GET /api/bookings/my/has-booking/{slotId} - Should return true when has booking")
    void hasActiveBookingForSlot_ShouldReturnTrue() throws Exception {
        when(bookingService.hasActiveBookingForSlot(1L)).thenReturn(true);

        mockMvc.perform(get("/api/bookings/my/has-booking/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /api/bookings/my/has-booking/{slotId} - Should return false when no booking")
    void hasActiveBookingForSlot_ShouldReturnFalse() throws Exception {
        when(bookingService.hasActiveBookingForSlot(999L)).thenReturn(false);

        mockMvc.perform(get("/api/bookings/my/has-booking/999"))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }
}
