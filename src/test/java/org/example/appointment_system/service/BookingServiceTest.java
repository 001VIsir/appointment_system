package org.example.appointment_system.service;

import org.example.appointment_system.dto.request.BookingRequest;
import org.example.appointment_system.dto.response.BookingResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.BookingRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AppointmentSlotRepository slotRepository;

    @Mock
    private AppointmentTaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @InjectMocks
    private BookingService bookingService;

    private User regularUser;
    private User merchantUser;
    private MerchantProfile merchantProfile;
    private ServiceItem serviceItem;
    private AppointmentTask appointmentTask;
    private AppointmentSlot availableSlot;
    private AppointmentSlot fullSlot;
    private Booking pendingBooking;
    private Booking confirmedBooking;
    private Booking cancelledBooking;
    private CustomUserDetails regularUserDetails;
    private CustomUserDetails merchantUserDetails;

    @BeforeEach
    void setUp() {
        // Create users
        regularUser = new User("testuser", "encodedPassword", "user@test.com", UserRole.USER);
        regularUser.setId(1L);

        merchantUser = new User("testmerchant", "encodedPassword", "merchant@test.com", UserRole.MERCHANT);
        merchantUser.setId(2L);

        // Create merchant profile
        merchantProfile = new MerchantProfile(merchantUser, "Test Business");
        merchantProfile.setId(100L);

        // Create service item
        serviceItem = new ServiceItem(merchantProfile, "Test Service", "Description",
            ServiceCategory.GENERAL, 30, BigDecimal.valueOf(50.00), true);
        serviceItem.setId(200L);

        // Create appointment task
        appointmentTask = new AppointmentTask(serviceItem, "Task Title", LocalDate.now(), 10);
        appointmentTask.setId(300L);
        appointmentTask.setActive(true);

        // Create slots
        availableSlot = new AppointmentSlot(appointmentTask, LocalTime.of(9, 0), LocalTime.of(9, 30), 5);
        availableSlot.setId(400L);
        availableSlot.setBookedCount(2); // 2 of 5 booked, has capacity

        fullSlot = new AppointmentSlot(appointmentTask, LocalTime.of(10, 0), LocalTime.of(10, 30), 5);
        fullSlot.setId(401L);
        fullSlot.setBookedCount(5); // Full

        // Create bookings
        pendingBooking = new Booking(regularUser, availableSlot, "Test remark");
        pendingBooking.setId(500L);
        setBookingVersion(pendingBooking, 0L);

        confirmedBooking = new Booking(regularUser, availableSlot, BookingStatus.CONFIRMED, null);
        confirmedBooking.setId(501L);
        setBookingVersion(confirmedBooking, 0L);

        cancelledBooking = new Booking(regularUser, availableSlot, BookingStatus.CANCELLED, null);
        cancelledBooking.setId(502L);
        setBookingVersion(cancelledBooking, 0L);

        // Create user details
        regularUserDetails = new CustomUserDetails(regularUser);
        merchantUserDetails = new CustomUserDetails(merchantUser);
    }

    private void setBookingVersion(Booking booking, Long version) {
        try {
            var field = Booking.class.getDeclaredField("version");
            field.setAccessible(true);
            field.set(booking, version);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    @DisplayName("createBooking tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void createBooking_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(slotRepository.findById(400L)).thenReturn(Optional.of(availableSlot));
            when(bookingRepository.existsActiveBookingByUserIdAndSlotId(1L, 400L)).thenReturn(false);
            when(slotRepository.save(any(AppointmentSlot.class))).thenReturn(availableSlot);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setId(500L);
                return booking;
            });

            BookingRequest request = BookingRequest.builder()
                .slotId(400L)
                .remark("Test remark")
                .build();

            // Act
            BookingResponse response = bookingService.createBooking(request);

            // Assert
            assertNotNull(response);
            assertEquals(400L, response.getSlotId());
            assertEquals("Test remark", response.getRemark());
            assertEquals(BookingStatus.PENDING, response.getStatus());
            assertEquals(1L, response.getUserId());

            verify(slotRepository).save(any(AppointmentSlot.class));
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when slot not found")
        void createBooking_SlotNotFound_ThrowsException() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(slotRepository.findById(999L)).thenReturn(Optional.empty());

            BookingRequest request = BookingRequest.builder()
                .slotId(999L)
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request)
            );
            assertTrue(exception.getMessage().contains("Slot not found"));
        }

        @Test
        @DisplayName("Should throw exception when slot is full")
        void createBooking_SlotFull_ThrowsException() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(slotRepository.findById(401L)).thenReturn(Optional.of(fullSlot));

            BookingRequest request = BookingRequest.builder()
                .slotId(401L)
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request)
            );
            assertTrue(exception.getMessage().contains("fully booked"));
        }

        @Test
        @DisplayName("Should throw exception when duplicate booking exists")
        void createBooking_DuplicateBooking_ThrowsException() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(slotRepository.findById(400L)).thenReturn(Optional.of(availableSlot));
            when(bookingRepository.existsActiveBookingByUserIdAndSlotId(1L, 400L)).thenReturn(true);

            BookingRequest request = BookingRequest.builder()
                .slotId(400L)
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request)
            );
            assertTrue(exception.getMessage().contains("already have an active booking"));
        }

        @Test
        @DisplayName("Should throw exception when no authenticated user")
        void createBooking_NoAuth_ThrowsException() {
            // Arrange
            clearSecurityContext();

            BookingRequest request = BookingRequest.builder()
                .slotId(400L)
                .build();

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
            );
            assertTrue(exception.getMessage().contains("No authenticated user"));
        }
    }

    @Nested
    @DisplayName("createBookingForUser tests")
    class CreateBookingForUserTests {

        @Test
        @DisplayName("Should create booking for another user successfully")
        void createBookingForUser_Success() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(slotRepository.findById(400L)).thenReturn(Optional.of(availableSlot));
            when(bookingRepository.existsActiveBookingByUserIdAndSlotId(1L, 400L)).thenReturn(false);
            when(slotRepository.save(any(AppointmentSlot.class))).thenReturn(availableSlot);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setId(500L);
                return booking;
            });

            BookingRequest request = BookingRequest.builder()
                .slotId(400L)
                .remark("Merchant created booking")
                .build();

            // Act
            BookingResponse response = bookingService.createBookingForUser(1L, request);

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getUserId());
            assertEquals("Merchant created booking", response.getRemark());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void createBookingForUser_UserNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            BookingRequest request = BookingRequest.builder()
                .slotId(400L)
                .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBookingForUser(999L, request)
            );
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }

    @Nested
    @DisplayName("cancelBooking tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void cancelBooking_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByIdAndUserId(500L, 1L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(slotRepository.save(any(AppointmentSlot.class))).thenReturn(availableSlot);

            // Act
            BookingResponse response = bookingService.cancelBooking(500L);

            // Assert
            assertEquals(BookingStatus.CANCELLED, response.getStatus());
            verify(slotRepository).save(any(AppointmentSlot.class));
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void cancelBooking_NotFound_ThrowsException() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(999L)
            );
            assertTrue(exception.getMessage().contains("Booking not found"));
        }

        @Test
        @DisplayName("Should throw exception when booking already cancelled")
        void cancelBooking_AlreadyCancelled_ThrowsException() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByIdAndUserId(502L, 1L)).thenReturn(Optional.of(cancelledBooking));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(502L)
            );
            assertTrue(exception.getMessage().contains("cannot be cancelled"));
        }
    }

    @Nested
    @DisplayName("cancelBookingByMerchant tests")
    class CancelBookingByMerchantTests {

        @Test
        @DisplayName("Should cancel booking by merchant successfully")
        void cancelBookingByMerchant_Success() {
            // Arrange
            when(bookingRepository.findById(500L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(slotRepository.save(any(AppointmentSlot.class))).thenReturn(availableSlot);

            // Act
            BookingResponse response = bookingService.cancelBookingByMerchant(500L, 100L);

            // Assert
            assertEquals(BookingStatus.CANCELLED, response.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void cancelBookingByMerchant_NotFound_ThrowsException() {
            // Arrange
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBookingByMerchant(999L, 100L)
            );
            assertTrue(exception.getMessage().contains("Booking not found"));
        }

        @Test
        @DisplayName("Should throw exception when booking belongs to different merchant")
        void cancelBookingByMerchant_WrongMerchant_ThrowsException() {
            // Arrange
            when(bookingRepository.findById(500L)).thenReturn(Optional.of(pendingBooking));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBookingByMerchant(500L, 999L)
            );
            assertTrue(exception.getMessage().contains("does not belong"));
        }
    }

    @Nested
    @DisplayName("confirmBooking tests")
    class ConfirmBookingTests {

        @Test
        @DisplayName("Should confirm booking successfully")
        void confirmBooking_Success() {
            // Arrange
            when(bookingRepository.findById(500L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BookingResponse response = bookingService.confirmBooking(500L, 100L);

            // Assert
            assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when booking already confirmed")
        void confirmBooking_AlreadyConfirmed_ThrowsException() {
            // Arrange
            when(bookingRepository.findById(501L)).thenReturn(Optional.of(confirmedBooking));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.confirmBooking(501L, 100L)
            );
            assertTrue(exception.getMessage().contains("cannot be confirmed"));
        }
    }

    @Nested
    @DisplayName("completeBooking tests")
    class CompleteBookingTests {

        @Test
        @DisplayName("Should complete booking successfully")
        void completeBooking_Success() {
            // Arrange
            when(bookingRepository.findById(501L)).thenReturn(Optional.of(confirmedBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BookingResponse response = bookingService.completeBooking(501L, 100L);

            // Assert
            assertEquals(BookingStatus.COMPLETED, response.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when booking not confirmed")
        void completeBooking_NotConfirmed_ThrowsException() {
            // Arrange
            when(bookingRepository.findById(500L)).thenReturn(Optional.of(pendingBooking));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.completeBooking(500L, 100L)
            );
            assertTrue(exception.getMessage().contains("cannot be completed"));
        }
    }

    @Nested
    @DisplayName("getBookingById tests")
    class GetBookingByIdTests {

        @Test
        @DisplayName("Should return booking when exists for user")
        void getBookingById_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByIdAndUserId(500L, 1L)).thenReturn(Optional.of(pendingBooking));

            // Act
            Optional<BookingResponse> response = bookingService.getBookingById(500L);

            // Assert
            assertTrue(response.isPresent());
            assertEquals(500L, response.get().getId());
        }

        @Test
        @DisplayName("Should return empty when booking not found")
        void getBookingById_NotFound() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            // Act
            Optional<BookingResponse> response = bookingService.getBookingById(999L);

            // Assert
            assertFalse(response.isPresent());
        }
    }

    @Nested
    @DisplayName("getMyBookings tests")
    class GetMyBookingsTests {

        @Test
        @DisplayName("Should return all user bookings")
        void getMyBookings_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByUserId(1L)).thenReturn(List.of(pendingBooking, confirmedBooking));

            // Act
            List<BookingResponse> responses = bookingService.getMyBookings();

            // Assert
            assertEquals(2, responses.size());
        }

        @Test
        @DisplayName("Should return empty list when no bookings")
        void getMyBookings_Empty() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByUserId(1L)).thenReturn(List.of());

            // Act
            List<BookingResponse> responses = bookingService.getMyBookings();

            // Assert
            assertTrue(responses.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMyBookings with pagination tests")
    class GetMyBookingsPagedTests {

        @Test
        @DisplayName("Should return paged user bookings")
        void getMyBookings_Paged() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            Page<Booking> bookingPage = new PageImpl<>(List.of(pendingBooking));
            when(bookingRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(bookingPage);

            // Act
            Page<BookingResponse> response = bookingService.getMyBookings(PageRequest.of(0, 10));

            // Assert
            assertEquals(1, response.getContent().size());
        }
    }

    @Nested
    @DisplayName("getMyBookingsByStatus tests")
    class GetMyBookingsByStatusTests {

        @Test
        @DisplayName("Should return bookings filtered by status")
        void getMyBookingsByStatus_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByUserIdAndStatus(1L, BookingStatus.PENDING))
                .thenReturn(List.of(pendingBooking));

            // Act
            List<BookingResponse> responses = bookingService.getMyBookingsByStatus(BookingStatus.PENDING);

            // Assert
            assertEquals(1, responses.size());
            assertEquals(BookingStatus.PENDING, responses.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("getMyActiveBookings tests")
    class GetMyActiveBookingsTests {

        @Test
        @DisplayName("Should return active bookings")
        void getMyActiveBookings_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(pendingBooking, confirmedBooking));

            // Act
            List<BookingResponse> responses = bookingService.getMyActiveBookings();

            // Assert
            assertEquals(2, responses.size());
        }
    }

    @Nested
    @DisplayName("getBookingsByMerchant tests")
    class GetBookingsByMerchantTests {

        @Test
        @DisplayName("Should return merchant bookings")
        void getBookingsByMerchant_Success() {
            // Arrange
            when(bookingRepository.findByMerchantId(100L)).thenReturn(List.of(pendingBooking));

            // Act
            List<BookingResponse> responses = bookingService.getBookingsByMerchant(100L);

            // Assert
            assertEquals(1, responses.size());
        }

        @Test
        @DisplayName("Should return paged merchant bookings")
        void getBookingsByMerchant_Paged() {
            // Arrange
            Page<Booking> bookingPage = new PageImpl<>(List.of(pendingBooking));
            when(bookingRepository.findByMerchantId(eq(100L), any(Pageable.class))).thenReturn(bookingPage);

            // Act
            Page<BookingResponse> response = bookingService.getBookingsByMerchant(100L, PageRequest.of(0, 10));

            // Assert
            assertEquals(1, response.getContent().size());
        }

        @Test
        @DisplayName("Should return bookings filtered by status")
        void getBookingsByMerchantAndStatus_Success() {
            // Arrange
            when(bookingRepository.findByMerchantIdAndStatus(100L, BookingStatus.PENDING))
                .thenReturn(List.of(pendingBooking));

            // Act
            List<BookingResponse> responses = bookingService.getBookingsByMerchantAndStatus(100L, BookingStatus.PENDING);

            // Assert
            assertEquals(1, responses.size());
            assertEquals(BookingStatus.PENDING, responses.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("getBookingsByTask tests")
    class GetBookingsByTaskTests {

        @Test
        @DisplayName("Should return task bookings")
        void getBookingsByTask_Success() {
            // Arrange
            when(bookingRepository.findByTaskId(300L)).thenReturn(List.of(pendingBooking));

            // Act
            List<BookingResponse> responses = bookingService.getBookingsByTask(300L);

            // Assert
            assertEquals(1, responses.size());
        }

        @Test
        @DisplayName("Should return paged task bookings")
        void getBookingsByTask_Paged() {
            // Arrange
            Page<Booking> bookingPage = new PageImpl<>(List.of(pendingBooking));
            when(bookingRepository.findByTaskId(eq(300L), any(Pageable.class))).thenReturn(bookingPage);

            // Act
            Page<BookingResponse> response = bookingService.getBookingsByTask(300L, PageRequest.of(0, 10));

            // Assert
            assertEquals(1, response.getContent().size());
        }
    }

    @Nested
    @DisplayName("getAvailableSlots tests")
    class GetAvailableSlotsTests {

        @Test
        @DisplayName("Should return all slots for task")
        void getAvailableSlots_Success() {
            // Arrange
            when(taskRepository.findById(300L)).thenReturn(Optional.of(appointmentTask));
            when(slotRepository.findByTaskOrderByStartTimeAsc(appointmentTask))
                .thenReturn(List.of(availableSlot, fullSlot));

            // Act
            List<SlotResponse> responses = bookingService.getAvailableSlots(300L);

            // Assert
            assertEquals(2, responses.size());
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void getAvailableSlots_TaskNotFound_ThrowsException() {
            // Arrange
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.getAvailableSlots(999L)
            );
            assertTrue(exception.getMessage().contains("Task not found"));
        }
    }

    @Nested
    @DisplayName("getSlotsWithCapacity tests")
    class GetSlotsWithCapacityTests {

        @Test
        @DisplayName("Should return only slots with capacity")
        void getSlotsWithCapacity_Success() {
            // Arrange
            when(taskRepository.findById(300L)).thenReturn(Optional.of(appointmentTask));
            when(slotRepository.findAvailableSlotsByTask(appointmentTask))
                .thenReturn(List.of(availableSlot)); // only availableSlot has capacity

            // Act
            List<SlotResponse> responses = bookingService.getSlotsWithCapacity(300L);

            // Assert
            assertEquals(1, responses.size());
            assertTrue(responses.get(0).getHasCapacity());
        }
    }

    @Nested
    @DisplayName("getSlotById tests")
    class GetSlotByIdTests {

        @Test
        @DisplayName("Should return slot when exists")
        void getSlotById_Success() {
            // Arrange
            when(slotRepository.findById(400L)).thenReturn(Optional.of(availableSlot));

            // Act
            Optional<SlotResponse> response = bookingService.getSlotById(400L);

            // Assert
            assertTrue(response.isPresent());
            assertEquals(400L, response.get().getId());
            assertEquals(5, response.get().getCapacity());
            assertEquals(2, response.get().getBookedCount());
            assertEquals(3, response.get().getAvailableCount());
            assertTrue(response.get().getHasCapacity());
        }

        @Test
        @DisplayName("Should return empty when slot not found")
        void getSlotById_NotFound() {
            // Arrange
            when(slotRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            Optional<SlotResponse> response = bookingService.getSlotById(999L);

            // Assert
            assertFalse(response.isPresent());
        }
    }

    @Nested
    @DisplayName("countOperations tests")
    class CountOperationsTests {

        @Test
        @DisplayName("Should count user bookings")
        void countMyBookings_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.countByUserId(1L)).thenReturn(5L);

            // Act
            long count = bookingService.countMyBookings();

            // Assert
            assertEquals(5L, count);
        }

        @Test
        @DisplayName("Should count active user bookings")
        void countMyActiveBookings_Success() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.PENDING)).thenReturn(2L);
            when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(3L);

            // Act
            long count = bookingService.countMyActiveBookings();

            // Assert
            assertEquals(5L, count);
        }

        @Test
        @DisplayName("Should count merchant bookings")
        void countBookingsByMerchant_Success() {
            // Arrange
            when(bookingRepository.countByMerchantId(100L)).thenReturn(10L);

            // Act
            long count = bookingService.countBookingsByMerchant(100L);

            // Assert
            assertEquals(10L, count);
        }

        @Test
        @DisplayName("Should count active task bookings")
        void countActiveBookingsByTask_Success() {
            // Arrange
            when(bookingRepository.countActiveByTaskId(300L)).thenReturn(8L);

            // Act
            long count = bookingService.countActiveBookingsByTask(300L);

            // Assert
            assertEquals(8L, count);
        }
    }

    @Nested
    @DisplayName("hasActiveBookingForSlot tests")
    class HasActiveBookingForSlotTests {

        @Test
        @DisplayName("Should return true when active booking exists")
        void hasActiveBookingForSlot_True() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.existsActiveBookingByUserIdAndSlotId(1L, 400L)).thenReturn(true);

            // Act
            boolean result = bookingService.hasActiveBookingForSlot(400L);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when no active booking")
        void hasActiveBookingForSlot_False() {
            // Arrange
            setSecurityContext(regularUserDetails);
            when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.existsActiveBookingByUserIdAndSlotId(1L, 400L)).thenReturn(false);

            // Act
            boolean result = bookingService.hasActiveBookingForSlot(400L);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("SlotResponse mapping tests")
    class SlotResponseMappingTests {

        @Test
        @DisplayName("Should correctly map slot with capacity")
        void mapSlotToResponse_WithCapacity() {
            // Arrange
            when(slotRepository.findById(400L)).thenReturn(Optional.of(availableSlot));

            // Act
            Optional<SlotResponse> response = bookingService.getSlotById(400L);

            // Assert
            assertTrue(response.isPresent());
            SlotResponse slot = response.get();
            assertEquals(400L, slot.getId());
            assertEquals(LocalTime.of(9, 0), slot.getStartTime());
            assertEquals(LocalTime.of(9, 30), slot.getEndTime());
            assertEquals(5, slot.getCapacity());
            assertEquals(2, slot.getBookedCount());
            assertEquals(3, slot.getAvailableCount());
            assertTrue(slot.getHasCapacity());
        }

        @Test
        @DisplayName("Should correctly map full slot")
        void mapSlotToResponse_Full() {
            // Arrange
            when(slotRepository.findById(401L)).thenReturn(Optional.of(fullSlot));

            // Act
            Optional<SlotResponse> response = bookingService.getSlotById(401L);

            // Assert
            assertTrue(response.isPresent());
            SlotResponse slot = response.get();
            assertEquals(0, slot.getAvailableCount());
            assertFalse(slot.getHasCapacity());
        }
    }
}
