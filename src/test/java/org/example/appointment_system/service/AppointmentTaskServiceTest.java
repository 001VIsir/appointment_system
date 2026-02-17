package org.example.appointment_system.service;

import org.example.appointment_system.dto.request.AppointmentSlotRequest;
import org.example.appointment_system.dto.request.AppointmentTaskRequest;
import org.example.appointment_system.dto.response.AppointmentTaskResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentTaskService.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentTaskServiceTest {

    @Mock
    private AppointmentTaskRepository taskRepository;

    @Mock
    private AppointmentSlotRepository slotRepository;

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AppointmentTaskService taskService;

    private User testUser;
    private MerchantProfile testMerchant;
    private ServiceItem testService;
    private AppointmentTask testTask;
    private AppointmentSlot testSlot;
    private CustomUserDetails userDetails;
    private AppointmentTaskRequest validTaskRequest;
    private AppointmentSlotRequest validSlotRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("merchant", "password", "merchant@test.com", UserRole.MERCHANT);
        testUser.setEnabled(true);
        setUserId(testUser, 1L);

        testMerchant = new MerchantProfile(testUser, "Test Salon", "Test Description", "1234567890", "Test Address", null);
        setMerchantId(testMerchant, 1L);

        testService = new ServiceItem(testMerchant, "Haircut", "Hair cutting service", ServiceCategory.BEAUTY, 30, new BigDecimal("25.00"), true);
        setServiceId(testService, 1L);

        testTask = new AppointmentTask(testService, "Morning Appointments", "Morning slots", LocalDate.now().plusDays(7), 10, true);
        setTaskId(testTask, 1L);

        testSlot = new AppointmentSlot(testTask, LocalTime.of(9, 0), LocalTime.of(10, 0), 2, 0);
        setSlotId(testSlot, 1L);

        userDetails = new CustomUserDetails(testUser);

        validTaskRequest = AppointmentTaskRequest.builder()
            .serviceId(1L)
            .title("Morning Appointments")
            .description("Morning slots")
            .taskDate(LocalDate.now().plusDays(7))
            .totalCapacity(10)
            .active(true)
            .build();

        validSlotRequest = AppointmentSlotRequest.builder()
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .capacity(2)
            .build();
    }

    // Helper methods to set IDs using reflection
    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setMerchantId(MerchantProfile merchant, Long id) {
        try {
            var field = MerchantProfile.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(merchant, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setServiceId(ServiceItem service, Long id) {
        try {
            var field = ServiceItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(service, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setTaskId(AppointmentTask task, Long id) {
        try {
            var field = AppointmentTask.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(task, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSlotId(AppointmentSlot slot, Long id) {
        try {
            var field = AppointmentSlot.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(slot, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockSecurityContext() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        }
    }

    private void setupAuthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(merchantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testMerchant));
        SecurityContextHolder.setContext(securityContext);
    }

    // ============================================
    // Task Creation Tests
    // ============================================

    @Nested
    @DisplayName("createTask")
    class CreateTaskTests {

        @Test
        @DisplayName("should create task successfully when service belongs to merchant")
        void createTask_withValidRequest_shouldCreateTask() {
            // Given
            setupAuthenticatedUser();
            when(serviceItemRepository.findByIdAndMerchant(1L, testMerchant))
                .thenReturn(Optional.of(testService));
            when(taskRepository.existsByServiceIdAndTaskDate(1L, validTaskRequest.getTaskDate()))
                .thenReturn(false);
            when(taskRepository.save(any(AppointmentTask.class))).thenReturn(testTask);
            when(slotRepository.countByTask(any(AppointmentTask.class))).thenReturn(0L);
            when(slotRepository.sumCapacityByTaskId(1L)).thenReturn(0);
            when(slotRepository.sumBookedCountByTaskId(1L)).thenReturn(0);

            // When
            AppointmentTaskResponse response = taskService.createTask(validTaskRequest);

            // Then
            assertNotNull(response);
            assertEquals("Morning Appointments", response.getTitle());
            verify(taskRepository).save(any(AppointmentTask.class));
        }

        @Test
        @DisplayName("should throw exception when service not found")
        void createTask_whenServiceNotFound_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            when(serviceItemRepository.findByIdAndMerchant(1L, testMerchant))
                .thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.createTask(validTaskRequest));
        }

        @Test
        @DisplayName("should throw exception when task already exists for date")
        void createTask_whenTaskExistsForDate_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            when(serviceItemRepository.findByIdAndMerchant(1L, testMerchant))
                .thenReturn(Optional.of(testService));
            when(taskRepository.existsByServiceIdAndTaskDate(1L, validTaskRequest.getTaskDate()))
                .thenReturn(true);

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.createTask(validTaskRequest));
        }
    }

    // ============================================
    // Task Update Tests
    // ============================================

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {

        @Test
        @DisplayName("should update task successfully")
        void updateTask_withValidRequest_shouldUpdateTask() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(AppointmentTask.class))).thenReturn(testTask);
            when(slotRepository.countByTask(any(AppointmentTask.class))).thenReturn(0L);
            when(slotRepository.sumCapacityByTaskId(1L)).thenReturn(0);
            when(slotRepository.sumBookedCountByTaskId(1L)).thenReturn(0);

            // When
            AppointmentTaskResponse response = taskService.updateTask(1L, validTaskRequest);

            // Then
            assertNotNull(response);
            verify(taskRepository).save(any(AppointmentTask.class));
        }

        @Test
        @DisplayName("should throw exception when task not found")
        void updateTask_whenTaskNotFound_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(1L, validTaskRequest));
        }

        @Test
        @DisplayName("should throw exception when task not owned by merchant")
        void updateTask_whenTaskNotOwned_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            MerchantProfile otherMerchant = new MerchantProfile();
            setMerchantId(otherMerchant, 999L);
            ServiceItem otherService = new ServiceItem();
            otherService.setMerchant(otherMerchant);
            testTask.setService(otherService);

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(1L, validTaskRequest));
        }
    }

    // ============================================
    // Task Delete Tests
    // ============================================

    @Nested
    @DisplayName("deleteTask")
    class DeleteTaskTests {

        @Test
        @DisplayName("should soft delete task successfully")
        void deleteTask_whenTaskExists_shouldSoftDelete() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(AppointmentTask.class))).thenReturn(testTask);

            // When
            taskService.deleteTask(1L);

            // Then
            assertFalse(testTask.getActive());
            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("should throw exception when task not found")
        void deleteTask_whenTaskNotFound_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.deleteTask(1L));
        }
    }

    // ============================================
    // Task Reactivate Tests
    // ============================================

    @Nested
    @DisplayName("reactivateTask")
    class ReactivateTaskTests {

        @Test
        @DisplayName("should reactivate task successfully")
        void reactivateTask_whenTaskExists_shouldReactivate() {
            // Given
            setupAuthenticatedUser();
            testTask.setActive(false);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(AppointmentTask.class))).thenReturn(testTask);
            when(slotRepository.countByTask(any(AppointmentTask.class))).thenReturn(0L);
            when(slotRepository.sumCapacityByTaskId(1L)).thenReturn(0);
            when(slotRepository.sumBookedCountByTaskId(1L)).thenReturn(0);

            // When
            AppointmentTaskResponse response = taskService.reactivateTask(1L);

            // Then
            assertTrue(response.getActive());
            verify(taskRepository).save(testTask);
        }
    }

    // ============================================
    // Task Query Tests
    // ============================================

    @Nested
    @DisplayName("getTaskById")
    class GetTaskByIdTests {

        @Test
        @DisplayName("should return task when owned by merchant")
        void getTaskById_whenOwnedByMerchant_shouldReturnTask() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(slotRepository.countByTask(any(AppointmentTask.class))).thenReturn(0L);
            when(slotRepository.sumCapacityByTaskId(1L)).thenReturn(0);
            when(slotRepository.sumBookedCountByTaskId(1L)).thenReturn(0);

            // When
            Optional<AppointmentTaskResponse> result = taskService.getTaskById(1L);

            // Then
            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
        }

        @Test
        @DisplayName("should return empty when not owned by merchant")
        void getTaskById_whenNotOwnedByMerchant_shouldReturnEmpty() {
            // Given
            setupAuthenticatedUser();
            MerchantProfile otherMerchant = new MerchantProfile();
            setMerchantId(otherMerchant, 999L);
            ServiceItem otherService = new ServiceItem();
            otherService.setMerchant(otherMerchant);
            testTask.setService(otherService);

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // When
            Optional<AppointmentTaskResponse> result = taskService.getTaskById(1L);

            // Then
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("getTaskByIdPublic")
    class GetTaskByIdPublicTests {

        @Test
        @DisplayName("should return task when active")
        void getTaskByIdPublic_whenActive_shouldReturnTask() {
            // Given
            when(taskRepository.findByIdAndActive(1L, true)).thenReturn(Optional.of(testTask));
            when(slotRepository.countByTask(any(AppointmentTask.class))).thenReturn(0L);
            when(slotRepository.sumCapacityByTaskId(1L)).thenReturn(0);
            when(slotRepository.sumBookedCountByTaskId(1L)).thenReturn(0);

            // When
            Optional<AppointmentTaskResponse> result = taskService.getTaskByIdPublic(1L);

            // Then
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("should return empty when inactive")
        void getTaskByIdPublic_whenInactive_shouldReturnEmpty() {
            // Given
            when(taskRepository.findByIdAndActive(1L, true)).thenReturn(Optional.empty());

            // When
            Optional<AppointmentTaskResponse> result = taskService.getTaskByIdPublic(1L);

            // Then
            assertFalse(result.isPresent());
        }
    }

    // ============================================
    // Slot CRUD Tests
    // ============================================

    @Nested
    @DisplayName("createSlot")
    class CreateSlotTests {

        @Test
        @DisplayName("should create slot successfully")
        void createSlot_withValidRequest_shouldCreateSlot() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(slotRepository.existsByTaskAndStartTime(testTask, validSlotRequest.getStartTime()))
                .thenReturn(false);
            when(slotRepository.save(any(AppointmentSlot.class))).thenReturn(testSlot);

            // When
            SlotResponse response = taskService.createSlot(1L, validSlotRequest);

            // Then
            assertNotNull(response);
            verify(slotRepository).save(any(AppointmentSlot.class));
        }

        @Test
        @DisplayName("should throw exception when end time is before start time")
        void createSlot_whenEndTimeBeforeStartTime_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            AppointmentSlotRequest invalidRequest = AppointmentSlotRequest.builder()
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(10, 0))
                .capacity(2)
                .build();

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.createSlot(1L, invalidRequest));
        }

        @Test
        @DisplayName("should throw exception when slot with same start time exists")
        void createSlot_whenDuplicateStartTime_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(slotRepository.existsByTaskAndStartTime(testTask, validSlotRequest.getStartTime()))
                .thenReturn(true);

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.createSlot(1L, validSlotRequest));
        }
    }

    @Nested
    @DisplayName("deleteSlot")
    class DeleteSlotTests {

        @Test
        @DisplayName("should delete slot when no bookings")
        void deleteSlot_whenNoBookings_shouldDelete() {
            // Given
            setupAuthenticatedUser();
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(slotRepository.findByIdAndTaskId(1L, 1L)).thenReturn(Optional.of(testSlot));
            doNothing().when(slotRepository).delete(testSlot);

            // When
            taskService.deleteSlot(1L, 1L);

            // Then
            verify(slotRepository).delete(testSlot);
        }

        @Test
        @DisplayName("should throw exception when slot has bookings")
        void deleteSlot_whenHasBookings_shouldThrowException() {
            // Given
            setupAuthenticatedUser();
            testSlot.setBookedCount(1);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(slotRepository.findByIdAndTaskId(1L, 1L)).thenReturn(Optional.of(testSlot));

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.deleteSlot(1L, 1L));
        }
    }

    // ============================================
    // Slot Query Tests
    // ============================================

    @Nested
    @DisplayName("getSlots")
    class GetSlotsTests {

        @Test
        @DisplayName("should return all slots for task")
        void getSlots_shouldReturnAllSlots() {
            // Given
            when(slotRepository.findByTaskIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(testSlot));

            // When
            List<SlotResponse> slots = taskService.getSlots(1L);

            // Then
            assertEquals(1, slots.size());
        }
    }

    @Nested
    @DisplayName("getSlotsPublic")
    class GetSlotsPublicTests {

        @Test
        @DisplayName("should return slots when task is active")
        void getSlotsPublic_whenTaskActive_shouldReturnSlots() {
            // Given
            when(taskRepository.findByIdAndActive(1L, true)).thenReturn(Optional.of(testTask));
            when(slotRepository.findByTaskIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(testSlot));

            // When
            List<SlotResponse> slots = taskService.getSlotsPublic(1L);

            // Then
            assertEquals(1, slots.size());
        }

        @Test
        @DisplayName("should throw exception when task not active")
        void getSlotsPublic_whenTaskInactive_shouldThrowException() {
            // Given
            when(taskRepository.findByIdAndActive(1L, true)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> taskService.getSlotsPublic(1L));
        }
    }

    @Nested
    @DisplayName("getSlotsWithCapacity")
    class GetSlotsWithCapacityTests {

        @Test
        @DisplayName("should return only slots with available capacity")
        void getSlotsWithCapacity_shouldReturnAvailableSlots() {
            // Given
            when(taskRepository.findByIdAndActive(1L, true)).thenReturn(Optional.of(testTask));
            when(slotRepository.findAvailableSlotsByTaskId(1L))
                .thenReturn(List.of(testSlot));

            // When
            List<SlotResponse> slots = taskService.getSlotsWithCapacity(1L);

            // Then
            assertEquals(1, slots.size());
            assertTrue(slots.get(0).getHasCapacity());
        }
    }
}
