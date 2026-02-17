package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.AppointmentSlotRequest;
import org.example.appointment_system.dto.request.AppointmentTaskRequest;
import org.example.appointment_system.dto.response.AppointmentTaskResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.service.AppointmentTaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for appointment task management operations.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>Merchant task management (CRUD)</li>
 *   <li>Merchant slot management</li>
 *   <li>Public task/slot viewing (for signed link access)</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * <h4>Merchant Endpoints (require MERCHANT/ADMIN role):</h4>
 * <ul>
 *   <li>POST /api/merchants/tasks - Create a new task</li>
 *   <li>GET /api/merchants/tasks - Get all tasks</li>
 *   <li>GET /api/merchants/tasks/{id} - Get task by ID</li>
 *   <li>PUT /api/merchants/tasks/{id} - Update task</li>
 *   <li>DELETE /api/merchants/tasks/{id} - Delete (deactivate) task</li>
 *   <li>POST /api/merchants/tasks/{id}/reactivate - Reactivate task</li>
 *   <li>POST /api/merchants/tasks/{id}/slots - Create slot(s)</li>
 *   <li>DELETE /api/merchants/tasks/{taskId}/slots/{slotId} - Delete slot</li>
 * </ul>
 *
 * <h4>Public Endpoints (for signed link access):</h4>
 * <ul>
 *   <li>GET /api/tasks/{id} - Get task details</li>
 *   <li>GET /api/tasks/{id}/slots - Get task slots</li>
 *   <li>GET /api/tasks/{id}/slots/available - Get available slots</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointment Task", description = "Appointment task and slot management APIs")
public class AppointmentTaskController {

    private final AppointmentTaskService taskService;

    // ============================================
    // Merchant Task Endpoints
    // ============================================

    /**
     * Create a new appointment task.
     *
     * <p>Creates a bookable task for a specific service on a specific date.</p>
     *
     * @param request the task creation request
     * @return the created task with 201 status
     */
    @PostMapping("/api/merchants/tasks")
    @Operation(
        summary = "Create an appointment task",
        description = "Creates a new appointment task for a merchant's service. " +
                      "Each service can have at most one task per date.",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Task created successfully",
                content = @Content(schema = @Schema(implementation = AppointmentTaskResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request or task already exists for this date"
            ),
            @ApiResponse(
                responseCode = "403",
                description = "User does not have MERCHANT role or no merchant profile"
            )
        }
    )
    public ResponseEntity<AppointmentTaskResponse> createTask(@Valid @RequestBody AppointmentTaskRequest request) {
        log.info("Creating task for service: {} on date: {}", request.getServiceId(), request.getTaskDate());
        AppointmentTaskResponse response = taskService.createTask(request);
        return ResponseEntity
            .created(URI.create("/api/tasks/" + response.getId()))
            .body(response);
    }

    /**
     * Get all tasks for the current merchant.
     *
     * <p>Returns both active and inactive tasks.</p>
     *
     * @return list of all tasks for the current merchant
     */
    @GetMapping("/api/merchants/tasks")
    @Operation(
        summary = "Get all merchant tasks",
        description = "Retrieves all appointment tasks for the current authenticated merchant."
    )
    public ResponseEntity<List<AppointmentTaskResponse>> getAllTasks() {
        List<AppointmentTaskResponse> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get active tasks for the current merchant.
     *
     * @return list of active tasks
     */
    @GetMapping("/api/merchants/tasks/active")
    @Operation(
        summary = "Get active merchant tasks",
        description = "Retrieves all active appointment tasks for the current merchant."
    )
    public ResponseEntity<List<AppointmentTaskResponse>> getActiveTasks() {
        List<AppointmentTaskResponse> tasks = taskService.getActiveTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get inactive (soft-deleted) tasks for the current merchant.
     *
     * @return list of inactive tasks
     */
    @GetMapping("/api/merchants/tasks/inactive")
    @Operation(
        summary = "Get inactive merchant tasks",
        description = "Retrieves all inactive (soft-deleted) appointment tasks for the current merchant."
    )
    public ResponseEntity<List<AppointmentTaskResponse>> getInactiveTasks() {
        List<AppointmentTaskResponse> tasks = taskService.getInactiveTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks for a specific service.
     *
     * @param serviceId the service ID
     * @return list of tasks for the service
     */
    @GetMapping("/api/merchants/tasks/by-service/{serviceId}")
    @Operation(
        summary = "Get tasks by service",
        description = "Retrieves all tasks for a specific service owned by the current merchant."
    )
    public ResponseEntity<List<AppointmentTaskResponse>> getTasksByService(
            @Parameter(description = "Service ID") @PathVariable Long serviceId) {
        List<AppointmentTaskResponse> tasks = taskService.getTasksByService(serviceId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks for a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of tasks in the date range
     */
    @GetMapping("/api/merchants/tasks/by-date")
    @Operation(
        summary = "Get tasks by date range",
        description = "Retrieves all active tasks for the current merchant within a date range."
    )
    public ResponseEntity<List<AppointmentTaskResponse>> getTasksByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AppointmentTaskResponse> tasks = taskService.getTasksByDateRange(startDate, endDate);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get a specific task by ID.
     *
     * @param id the task ID
     * @return the task or 404 if not found
     */
    @GetMapping("/api/merchants/tasks/{id}")
    @Operation(
        summary = "Get task by ID",
        description = "Retrieves a specific appointment task by ID. " +
                      "Only returns tasks owned by the current merchant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task found"),
        @ApiResponse(responseCode = "404", description = "Task not found or not owned by current merchant")
    })
    public ResponseEntity<AppointmentTaskResponse> getTaskById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return taskService.getTaskById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a task.
     *
     * @param id the task ID
     * @param request the update request
     * @return the updated task
     */
    @PutMapping("/api/merchants/tasks/{id}")
    @Operation(
        summary = "Update task",
        description = "Updates an appointment task. Only the merchant who owns the task can update it."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or date conflict"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<AppointmentTaskResponse> updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody AppointmentTaskRequest request) {
        log.info("Updating task {}: {}", id, request.getTitle());
        AppointmentTaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete (deactivate) a task.
     *
     * @param id the task ID
     * @return empty response with 204 status
     */
    @DeleteMapping("/api/merchants/tasks/{id}")
    @Operation(
        summary = "Delete task",
        description = "Soft deletes a task by setting active=false. " +
                      "The record remains in the database and can be reactivated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("Deleting task {}", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivate a previously deleted task.
     *
     * @param id the task ID
     * @return the reactivated task
     */
    @PostMapping("/api/merchants/tasks/{id}/reactivate")
    @Operation(
        summary = "Reactivate task",
        description = "Reactivates a previously soft-deleted task."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task reactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<AppointmentTaskResponse> reactivateTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("Reactivating task {}", id);
        AppointmentTaskResponse response = taskService.reactivateTask(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Count tasks for the current merchant.
     *
     * @param activeOnly if true, only count active tasks
     * @return the count of tasks
     */
    @GetMapping("/api/merchants/tasks/count")
    @Operation(
        summary = "Count tasks",
        description = "Returns the count of tasks for the current merchant."
    )
    public ResponseEntity<Long> countTasks(
            @Parameter(description = "Only count active tasks")
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        long count = taskService.countTasks(activeOnly);
        return ResponseEntity.ok(count);
    }

    // ============================================
    // Merchant Slot Endpoints
    // ============================================

    /**
     * Create time slots for a task.
     *
     * <p>Accepts either a single slot or an array of slots.</p>
     *
     * @param taskId the task ID
     * @param requests list of slot creation requests
     * @return list of created slots
     */
    @PostMapping("/api/merchants/tasks/{taskId}/slots")
    @Operation(
        summary = "Create time slots",
        description = "Creates time slots for an appointment task. " +
                      "Accepts either a single slot or an array of slots."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Slots created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or slot time conflict"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<List<SlotResponse>> createSlots(
            @Parameter(description = "Task ID") @PathVariable Long taskId,
            @Valid @RequestBody List<AppointmentSlotRequest> requests) {
        log.info("Creating {} slots for task {}", requests.size(), taskId);
        List<SlotResponse> slots = taskService.createSlots(taskId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(slots);
    }

    /**
     * Get all slots for a task (merchant view).
     *
     * @param taskId the task ID
     * @return list of all slots for the task
     */
    @GetMapping("/api/merchants/tasks/{taskId}/slots")
    @Operation(
        summary = "Get task slots (merchant)",
        description = "Retrieves all time slots for a task (merchant view)."
    )
    public ResponseEntity<List<SlotResponse>> getSlots(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        List<SlotResponse> slots = taskService.getSlots(taskId);
        return ResponseEntity.ok(slots);
    }

    /**
     * Delete a slot from a task.
     *
     * @param taskId the task ID
     * @param slotId the slot ID
     * @return empty response with 204 status
     */
    @DeleteMapping("/api/merchants/tasks/{taskId}/slots/{slotId}")
    @Operation(
        summary = "Delete slot",
        description = "Deletes a time slot from a task. " +
                      "Cannot delete slots that have existing bookings."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Slot deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Slot has existing bookings"),
        @ApiResponse(responseCode = "404", description = "Task or slot not found")
    })
    public ResponseEntity<Void> deleteSlot(
            @Parameter(description = "Task ID") @PathVariable Long taskId,
            @Parameter(description = "Slot ID") @PathVariable Long slotId) {
        log.info("Deleting slot {} from task {}", slotId, taskId);
        taskService.deleteSlot(taskId, slotId);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // Public Task Endpoint (for signed links)
    // ============================================

    /**
     * Get a task by ID (public access).
     *
     * <p>This endpoint is accessible without authentication for signed link access.</p>
     *
     * @param id the task ID
     * @return the task or 404 if not found
     */
    @GetMapping("/api/tasks/{id}")
    @Operation(
        summary = "Get task by ID (public)",
        description = "Retrieves an appointment task by ID. " +
                      "This is a public endpoint for signed link access."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task found"),
        @ApiResponse(responseCode = "404", description = "Task not found or not active")
    })
    public ResponseEntity<AppointmentTaskResponse> getTaskByIdPublic(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return taskService.getTaskByIdPublic(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Note: Public slot endpoints (GET /api/tasks/{taskId}/slots and GET /api/tasks/{taskId}/slots/available)
    // are provided by BookingController to avoid duplicate endpoint mappings.
}
