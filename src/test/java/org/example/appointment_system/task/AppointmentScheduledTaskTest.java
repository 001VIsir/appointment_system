package org.example.appointment_system.task;

import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentScheduledTask.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentScheduledTaskTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AppointmentTaskRepository taskRepository;

    @Mock
    private AppointmentSlotRepository slotRepository;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private AppointmentScheduledTask scheduledTask;

    private User testUser;
    private ServiceItem testService;
    private AppointmentTask testTask;
    private AppointmentSlot testSlot;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testService = new ServiceItem();
        testService.setId(1L);
        testService.setName("Test Service");

        testTask = new AppointmentTask();
        testTask.setId(1L);
        testTask.setService(testService);
        testTask.setTitle("Test Task");
        testTask.setTaskDate(LocalDate.now());
        testTask.setActive(true);

        testSlot = new AppointmentSlot();
        testSlot.setId(1L);
        testSlot.setTask(testTask);
        testSlot.setStartTime(LocalTime.now().minusHours(2));
        testSlot.setEndTime(LocalTime.now().minusHours(1));
        testSlot.setCapacity(10);
        testSlot.setBookedCount(1);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUser(testUser);
        testBooking.setSlot(testSlot);
        testBooking.setStatus(BookingStatus.PENDING);
    }

    @Nested
    @DisplayName("handleBookingTimeouts() Tests")
    class HandleBookingTimeoutsTests {

        @Test
        @DisplayName("Should cancel bookings for ended slots")
        void shouldCancelBookingsForEndedSlots() {
            // Arrange
            when(taskRepository.findByTaskDateAndActiveTrue(any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testTask));
            when(slotRepository.findByTask(any(AppointmentTask.class)))
                    .thenReturn(Arrays.asList(testSlot));
            when(bookingRepository.findBySlotIdAndStatus(anyLong(), eq(BookingStatus.PENDING)))
                    .thenReturn(Arrays.asList(testBooking));

            // Act
            scheduledTask.handleBookingTimeouts();

            // Assert
            verify(bookingRepository).save(any(Booking.class));
            verify(slotRepository).save(any(AppointmentSlot.class));
        }

        @Test
        @DisplayName("Should not cancel bookings for future slots")
        void shouldNotCancelBookingsForFutureSlots() {
            // Arrange - slot ends in the future
            testSlot.setEndTime(LocalTime.now().plusHours(2));

            when(taskRepository.findByTaskDateAndActiveTrue(any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testTask));
            when(slotRepository.findByTask(any(AppointmentTask.class)))
                    .thenReturn(Arrays.asList(testSlot));

            // Act
            scheduledTask.handleBookingTimeouts();

            // Assert
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should handle no active tasks gracefully")
        void shouldHandleNoActiveTasksGracefully() {
            // Arrange
            when(taskRepository.findByTaskDateAndActiveTrue(any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            scheduledTask.handleBookingTimeouts();

            // Assert
            verify(bookingRepository, never()).save(any(Booking.class));
            verify(slotRepository, never()).save(any(AppointmentSlot.class));
        }

        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            // Arrange
            when(taskRepository.findByTaskDateAndActiveTrue(any(LocalDate.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - should not throw
            scheduledTask.handleBookingTimeouts();

            // Verify it attempted to get tasks
            verify(taskRepository).findByTaskDateAndActiveTrue(any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("sendAppointmentReminders() Tests")
    class SendAppointmentRemindersTests {

        @Test
        @DisplayName("Should identify bookings needing reminders")
        void shouldIdentifyBookingsNeedingReminders() {
            // Arrange - slot is in 24 hours
            testSlot.setStartTime(LocalTime.now().plusHours(24));
            testSlot.setEndTime(LocalTime.now().plusHours(25));

            when(taskRepository.findByTaskDateAndActiveTrue(any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testTask));
            when(slotRepository.findByTaskAndTimeRange(any(AppointmentTask.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(Arrays.asList(testSlot));
            when(bookingRepository.findBySlotIdAndStatus(anyLong(), eq(BookingStatus.CONFIRMED)))
                    .thenReturn(Arrays.asList(testBooking));

            // Act
            scheduledTask.sendAppointmentReminders();

            // Assert
            verify(bookingRepository).findBySlotIdAndStatus(anyLong(), eq(BookingStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Should not send reminders for pending bookings")
        void shouldNotSendRemindersForPendingBookings() {
            // Arrange
            when(taskRepository.findByTaskDateAndActiveTrue(any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testTask));
            when(slotRepository.findByTaskAndTimeRange(any(AppointmentTask.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(Arrays.asList(testSlot));
            when(bookingRepository.findBySlotIdAndStatus(anyLong(), eq(BookingStatus.CONFIRMED)))
                    .thenReturn(Collections.emptyList());

            // Act
            scheduledTask.sendAppointmentReminders();

            // Assert
            verify(bookingRepository).findBySlotIdAndStatus(anyLong(), eq(BookingStatus.CONFIRMED));
        }
    }

    @Nested
    @DisplayName("autoCompleteAppointments() Tests")
    class AutoCompleteAppointmentsTests {

        @Test
        @DisplayName("Should auto-complete confirmed bookings for ended slots")
        void shouldAutoCompleteConfirmedBookings() {
            // Arrange - task is 3 days ago
            testTask.setTaskDate(LocalDate.now().minusDays(3));

            when(taskRepository.findByTaskDateBetweenAndActiveTrue(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testTask));
            when(slotRepository.findByTask(any(AppointmentTask.class)))
                    .thenReturn(Arrays.asList(testSlot));
            when(bookingRepository.findBySlotIdAndStatus(anyLong(), eq(BookingStatus.CONFIRMED)))
                    .thenReturn(Arrays.asList(testBooking));

            // Act
            scheduledTask.autoCompleteAppointments();

            // Assert
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should not complete pending bookings")
        void shouldNotCompletePendingBookings() {
            // Arrange
            testTask.setTaskDate(LocalDate.now().minusDays(3));

            when(taskRepository.findByTaskDateBetweenAndActiveTrue(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testTask));
            when(slotRepository.findByTask(any(AppointmentTask.class)))
                    .thenReturn(Arrays.asList(testSlot));
            when(bookingRepository.findBySlotIdAndStatus(anyLong(), eq(BookingStatus.CONFIRMED)))
                    .thenReturn(Collections.emptyList());

            // Act
            scheduledTask.autoCompleteAppointments();

            // Assert
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("generateDailyStatistics() Tests")
    class GenerateDailyStatisticsTests {

        @Test
        @DisplayName("Should generate daily statistics")
        void shouldGenerateDailyStatistics() {
            // Arrange
            doNothing().when(statisticsService).generateDailySummary(any(LocalDate.class));
            when(bookingRepository.findByCreatedAtBetween(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(taskRepository.findByTaskDate(any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            scheduledTask.generateDailyStatistics();

            // Assert
            verify(statisticsService).generateDailySummary(any(LocalDate.class));
        }

        @Test
        @DisplayName("Should handle exceptions in daily statistics")
        void shouldHandleExceptionsInDailyStatistics() {
            // Arrange
            doThrow(new RuntimeException("Redis error"))
                    .when(statisticsService).generateDailySummary(any(LocalDate.class));

            // Act & Assert - should not throw
            scheduledTask.generateDailyStatistics();

            // Verify it attempted to generate summary
            verify(statisticsService).generateDailySummary(any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("cleanupOldData() Tests")
    class CleanupOldDataTests {

        @Test
        @DisplayName("Should identify old data for cleanup")
        void shouldIdentifyOldDataForCleanup() {
            // Arrange - old completed booking
            testBooking.setStatus(BookingStatus.COMPLETED);
            testBooking.setCreatedAt(LocalDate.now().minusDays(100).atStartOfDay());

            when(bookingRepository.findByStatusIn(anyList()))
                    .thenReturn(Arrays.asList(testBooking));

            // Act
            scheduledTask.cleanupOldData();

            // Assert
            verify(bookingRepository).findByStatusIn(anyList());
        }

        @Test
        @DisplayName("Should handle no old data gracefully")
        void shouldHandleNoOldDataGracefully() {
            // Arrange
            when(bookingRepository.findByStatusIn(anyList()))
                    .thenReturn(Collections.emptyList());

            // Act
            scheduledTask.cleanupOldData();

            // Assert
            verify(bookingRepository).findByStatusIn(anyList());
        }
    }
}
