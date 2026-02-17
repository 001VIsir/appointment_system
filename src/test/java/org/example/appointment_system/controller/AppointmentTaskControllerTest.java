package org.example.appointment_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointment_system.dto.request.AppointmentSlotRequest;
import org.example.appointment_system.dto.request.AppointmentTaskRequest;
import org.example.appointment_system.dto.response.AppointmentTaskResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.service.AppointmentTaskService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
 * Unit tests for AppointmentTaskController using standalone MockMvc setup.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentTaskControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AppointmentTaskService taskService;

    @InjectMocks
    private AppointmentTaskController taskController;

    private AppointmentTaskRequest validTaskRequest;
    private AppointmentTaskResponse validTaskResponse;
    private AppointmentSlotRequest validSlotRequest;
    private SlotResponse validSlotResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        validTaskRequest = AppointmentTaskRequest.builder()
            .serviceId(1L)
            .title("Morning Appointments")
            .description("Available morning slots")
            .taskDate(LocalDate.now().plusDays(7))
            .totalCapacity(10)
            .active(true)
            .build();

        validTaskResponse = AppointmentTaskResponse.builder()
            .id(1L)
            .serviceId(1L)
            .serviceName("Haircut")
            .merchantId(1L)
            .merchantBusinessName("Test Salon")
            .title("Morning Appointments")
            .description("Available morning slots")
            .taskDate(LocalDate.now().plusDays(7))
            .totalCapacity(10)
            .slotCount(5)
            .totalSlotCapacity(10)
            .totalBookedCount(3)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        validSlotRequest = AppointmentSlotRequest.builder()
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .capacity(2)
            .build();

        validSlotResponse = SlotResponse.builder()
            .id(1L)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .capacity(2)
            .bookedCount(0)
            .availableCount(2)
            .hasCapacity(true)
            .build();
    }

    // ============================================
    // Merchant Task Endpoint Tests
    // ============================================

    @Nested
    @DisplayName("POST /api/merchants/tasks")
    class CreateTaskEndpointTests {

        @Test
        @DisplayName("should create task successfully and return 201")
        void createTask_withValidRequest_shouldReturn201() throws Exception {
            // Given
            when(taskService.createTask(any(AppointmentTaskRequest.class)))
                .thenReturn(validTaskResponse);

            // When/Then
            mockMvc.perform(post("/api/merchants/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.serviceId").value(1))
                .andExpect(jsonPath("$.serviceName").value("Haircut"))
                .andExpect(jsonPath("$.merchantId").value(1))
                .andExpect(jsonPath("$.title").value("Morning Appointments"))
                .andExpect(jsonPath("$.totalCapacity").value(10))
                .andExpect(jsonPath("$.active").value(true));

            verify(taskService).createTask(any(AppointmentTaskRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/tasks")
    class GetAllTasksEndpointTests {

        @Test
        @DisplayName("should return all tasks for merchant")
        void getAllTasks_whenTasksExist_shouldReturnList() throws Exception {
            // Given
            AppointmentTaskResponse task2 = AppointmentTaskResponse.builder()
                .id(2L)
                .serviceId(1L)
                .serviceName("Haircut")
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .title("Afternoon Appointments")
                .taskDate(LocalDate.now().plusDays(8))
                .totalCapacity(10)
                .slotCount(4)
                .totalSlotCapacity(8)
                .totalBookedCount(0)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            List<AppointmentTaskResponse> tasks = Arrays.asList(validTaskResponse, task2);
            when(taskService.getAllTasks()).thenReturn(tasks);

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Morning Appointments"))
                .andExpect(jsonPath("$[1].title").value("Afternoon Appointments"));
        }

        @Test
        @DisplayName("should return empty list when no tasks")
        void getAllTasks_whenNoTasks_shouldReturnEmptyList() throws Exception {
            // Given
            when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/tasks/active")
    class GetActiveTasksEndpointTests {

        @Test
        @DisplayName("should return only active tasks")
        void getActiveTasks_shouldReturnActiveOnly() throws Exception {
            // Given
            List<AppointmentTaskResponse> activeTasks = List.of(validTaskResponse);
            when(taskService.getActiveTasks()).thenReturn(activeTasks);

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/tasks/inactive")
    class GetInactiveTasksEndpointTests {

        @Test
        @DisplayName("should return only inactive tasks")
        void getInactiveTasks_shouldReturnInactiveOnly() throws Exception {
            // Given
            AppointmentTaskResponse inactiveTask = AppointmentTaskResponse.builder()
                .id(2L)
                .serviceId(1L)
                .serviceName("Haircut")
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .title("Old Task")
                .taskDate(LocalDate.now().minusDays(1))
                .totalCapacity(10)
                .slotCount(0)
                .totalSlotCapacity(0)
                .totalBookedCount(0)
                .active(false)
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now())
                .build();

            List<AppointmentTaskResponse> inactiveTasks = List.of(inactiveTask);
            when(taskService.getInactiveTasks()).thenReturn(inactiveTasks);

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/tasks/{id}")
    class GetTaskByIdEndpointTests {

        @Test
        @DisplayName("should return task when found")
        void getTaskById_whenFound_shouldReturn200() throws Exception {
            // Given
            when(taskService.getTaskById(1L))
                .thenReturn(Optional.of(validTaskResponse));

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Morning Appointments"));
        }

        @Test
        @DisplayName("should return 404 when task not found")
        void getTaskById_whenNotFound_shouldReturn404() throws Exception {
            // Given
            when(taskService.getTaskById(999L))
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/merchants/tasks/{id}")
    class UpdateTaskEndpointTests {

        @Test
        @DisplayName("should update task successfully and return 200")
        void updateTask_withValidRequest_shouldReturn200() throws Exception {
            // Given
            AppointmentTaskRequest updateRequest = AppointmentTaskRequest.builder()
                .serviceId(1L)
                .title("Updated Title")
                .description("Updated description")
                .taskDate(LocalDate.now().plusDays(10))
                .totalCapacity(15)
                .active(true)
                .build();

            AppointmentTaskResponse updatedResponse = AppointmentTaskResponse.builder()
                .id(1L)
                .serviceId(1L)
                .serviceName("Haircut")
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .title("Updated Title")
                .description("Updated description")
                .taskDate(LocalDate.now().plusDays(10))
                .totalCapacity(15)
                .slotCount(5)
                .totalSlotCapacity(10)
                .totalBookedCount(3)
                .active(true)
                .createdAt(validTaskResponse.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

            when(taskService.updateTask(eq(1L), any(AppointmentTaskRequest.class)))
                .thenReturn(updatedResponse);

            // When/Then
            mockMvc.perform(put("/api/merchants/tasks/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.totalCapacity").value(15));

            verify(taskService).updateTask(eq(1L), any(AppointmentTaskRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/merchants/tasks/{id}")
    class DeleteTaskEndpointTests {

        @Test
        @DisplayName("should delete task successfully and return 204")
        void deleteTask_shouldReturn204() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/merchants/tasks/1"))
                .andExpect(status().isNoContent());

            verify(taskService).deleteTask(1L);
        }
    }

    @Nested
    @DisplayName("POST /api/merchants/tasks/{id}/reactivate")
    class ReactivateTaskEndpointTests {

        @Test
        @DisplayName("should reactivate task successfully")
        void reactivateTask_shouldReturn200() throws Exception {
            // Given
            AppointmentTaskResponse reactivatedResponse = AppointmentTaskResponse.builder()
                .id(1L)
                .serviceId(1L)
                .serviceName("Haircut")
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .title("Morning Appointments")
                .active(true)
                .createdAt(validTaskResponse.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

            when(taskService.reactivateTask(1L))
                .thenReturn(reactivatedResponse);

            // When/Then
            mockMvc.perform(post("/api/merchants/tasks/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true));

            verify(taskService).reactivateTask(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/tasks/count")
    class CountTasksEndpointTests {

        @Test
        @DisplayName("should return total count when activeOnly is false")
        void countTasks_totalCount_shouldReturnCount() throws Exception {
            // Given
            when(taskService.countTasks(false)).thenReturn(5L);

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/count")
                    .param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

            verify(taskService).countTasks(false);
        }

        @Test
        @DisplayName("should return active count when activeOnly is true")
        void countTasks_activeOnly_shouldReturnActiveCount() throws Exception {
            // Given
            when(taskService.countTasks(true)).thenReturn(3L);

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/count")
                    .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

            verify(taskService).countTasks(true);
        }
    }

    // ============================================
    // Merchant Slot Endpoint Tests
    // ============================================

    @Nested
    @DisplayName("POST /api/merchants/tasks/{taskId}/slots")
    class CreateSlotsEndpointTests {

        @Test
        @DisplayName("should create slots successfully and return 201")
        void createSlots_withValidRequests_shouldReturn201() throws Exception {
            // Given
            List<AppointmentSlotRequest> requests = List.of(validSlotRequest);
            List<SlotResponse> responses = List.of(validSlotResponse);
            when(taskService.createSlots(eq(1L), anyList())).thenReturn(responses);

            // When/Then
            mockMvc.perform(post("/api/merchants/tasks/1/slots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("10:00:00"))
                .andExpect(jsonPath("$[0].capacity").value(2));

            verify(taskService).createSlots(eq(1L), anyList());
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/tasks/{taskId}/slots")
    class GetSlotsEndpointTests {

        @Test
        @DisplayName("should return all slots for task")
        void getSlots_whenSlotsExist_shouldReturnList() throws Exception {
            // Given
            SlotResponse slot2 = SlotResponse.builder()
                .id(2L)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .capacity(2)
                .bookedCount(1)
                .availableCount(1)
                .hasCapacity(true)
                .build();

            List<SlotResponse> slots = Arrays.asList(validSlotResponse, slot2);
            when(taskService.getSlots(1L)).thenReturn(slots);

            // When/Then
            mockMvc.perform(get("/api/merchants/tasks/1/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("DELETE /api/merchants/tasks/{taskId}/slots/{slotId}")
    class DeleteSlotEndpointTests {

        @Test
        @DisplayName("should delete slot successfully and return 204")
        void deleteSlot_shouldReturn204() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/merchants/tasks/1/slots/1"))
                .andExpect(status().isNoContent());

            verify(taskService).deleteSlot(1L, 1L);
        }
    }

    // ============================================
    // Public Endpoint Tests
    // ============================================

    @Nested
    @DisplayName("GET /api/tasks/{id} (public)")
    class GetTaskByIdPublicEndpointTests {

        @Test
        @DisplayName("should return task when found and active")
        void getTaskByIdPublic_whenFound_shouldReturn200() throws Exception {
            // Given
            when(taskService.getTaskByIdPublic(1L))
                .thenReturn(Optional.of(validTaskResponse));

            // When/Then
            mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Morning Appointments"));
        }

        @Test
        @DisplayName("should return 404 when task not found or inactive")
        void getTaskByIdPublic_whenNotFound_shouldReturn404() throws Exception {
            // Given
            when(taskService.getTaskByIdPublic(999L))
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());
        }
    }

    // Note: Public slot endpoints (GET /api/tasks/{taskId}/slots and GET /api/tasks/{taskId}/slots/available)
    // are tested in BookingControllerTest to avoid duplicate endpoint mappings.
}
