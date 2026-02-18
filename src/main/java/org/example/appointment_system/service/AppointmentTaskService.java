package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.AppointmentSlotRequest;
import org.example.appointment_system.dto.request.AppointmentTaskRequest;
import org.example.appointment_system.dto.response.AppointmentTaskResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 预约任务管理操作服务类。
 *
 * <p>处理预约任务和时段的CRUD操作，包括：</p>
 * <ul>
 *   <li>为商家服务创建预约任务</li>
 *   <li>管理任务内的时段</li>
 *   <li>软删除任务（设置active=false）</li>
 *   <li>获取商家和公开访问的任务</li>
 * </ul>
 *
 * <h3>安全性：</h3>
 * <p>任务创建和修改需要当前用户具有MERCHANT角色
 * 和现有的商家资料。用户只能访问和修改自己的任务。
 * 允许公开访问任务用于签名链接访问。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentTaskService {

    private final AppointmentTaskRepository taskRepository;
    private final AppointmentSlotRepository slotRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ServiceItemRepository serviceItemRepository;

    // ============================================
    // 任务CRUD操作
    // ============================================

    /**
     * Create a new appointment task for a service.
     *
     * <p>The service must belong to the current merchant.</p>
     *
     * @param request the task creation request
     * @return AppointmentTaskResponse containing the created task
     * @throws IllegalStateException if user doesn't have a merchant profile
     * @throws IllegalArgumentException if service not found or not owned by merchant
     */
    @Transactional
    public AppointmentTaskResponse createTask(AppointmentTaskRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        // 查找服务项目并验证所有权
        ServiceItem serviceItem = serviceItemRepository.findByIdAndMerchant(
                request.getServiceId(), merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service not found or not owned by current merchant"));

        // 检查此服务在此日期是否已存在任务
        if (taskRepository.existsByServiceIdAndTaskDate(request.getServiceId(), request.getTaskDate())) {
            log.warn("Task already exists for service {} on date {}",
                request.getServiceId(), request.getTaskDate());
            throw new IllegalArgumentException(
                "A task already exists for this service on the specified date");
        }

        AppointmentTask task = new AppointmentTask(
            serviceItem,
            request.getTitle(),
            request.getDescription(),
            request.getTaskDate(),
            request.getTotalCapacity(),
            request.getActive() != null ? request.getActive() : true
        );

        AppointmentTask savedTask = taskRepository.save(task);
        log.info("Created task '{}' for service {}: id={}",
            savedTask.getTitle(), serviceItem.getId(), savedTask.getId());

        return mapToResponse(savedTask);
    }

    /**
     * Update an existing appointment task.
     *
     * <p>Only the merchant who owns the task can update it.</p>
     *
     * @param taskId the ID of the task to update
     * @param request the update request
     * @return AppointmentTaskResponse containing the updated task
     * @throws IllegalArgumentException if task not found or not owned by current merchant
     */
    @Transactional
    public AppointmentTaskResponse updateTask(Long taskId, AppointmentTaskRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 通过服务 -> 商家链验证所有权
        if (!task.getService().getMerchant().getId().equals(merchantProfile.getId())) {
            throw new IllegalArgumentException("Task not found or not owned by current merchant");
        }

        // 如果更改日期，检查冲突
        if (!task.getTaskDate().equals(request.getTaskDate())) {
            if (taskRepository.existsByServiceIdAndTaskDate(
                    task.getService().getId(), request.getTaskDate())) {
                throw new IllegalArgumentException(
                    "A task already exists for this service on the specified date");
            }
        }

        // 更新字段
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setTaskDate(request.getTaskDate());
        task.setTotalCapacity(request.getTotalCapacity());
        if (request.getActive() != null) {
            task.setActive(request.getActive());
        }

        AppointmentTask updatedTask = taskRepository.save(task);
        log.info("Updated task {} for merchant {}", taskId, merchantProfile.getId());

        return mapToResponse(updatedTask);
    }

    /**
     * Soft delete a task by setting active=false.
     *
     * <p>Only the merchant who owns the task can delete it.</p>
     *
     * @param taskId the ID of the task to delete
     * @throws IllegalArgumentException if task not found or not owned by current merchant
     */
    @Transactional
    public void deleteTask(Long taskId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 验证所有权
        if (!task.getService().getMerchant().getId().equals(merchantProfile.getId())) {
            throw new IllegalArgumentException("Task not found or not owned by current merchant");
        }

        // 通过设置active=false进行软删除
        task.setActive(false);
        taskRepository.save(task);
        log.info("Soft deleted task {} for merchant {}", taskId, merchantProfile.getId());
    }

    /**
     * Reactivate a previously soft-deleted task.
     *
     * @param taskId the ID of the task to reactivate
     * @return AppointmentTaskResponse containing the reactivated task
     * @throws IllegalArgumentException if task not found or not owned by current merchant
     */
    @Transactional
    public AppointmentTaskResponse reactivateTask(Long taskId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 验证所有权
        if (!task.getService().getMerchant().getId().equals(merchantProfile.getId())) {
            throw new IllegalArgumentException("Task not found or not owned by current merchant");
        }

        task.setActive(true);
        AppointmentTask reactivatedTask = taskRepository.save(task);
        log.info("Reactivated task {} for merchant {}", taskId, merchantProfile.getId());

        return mapToResponse(reactivatedTask);
    }

    // ============================================
    // 任务查询操作
    // ============================================

    /**
     * Get a task by ID (for merchants).
     *
     * <p>Only returns tasks owned by the current merchant.</p>
     *
     * @param taskId the ID of the task
     * @return Optional containing AppointmentTaskResponse if found and owned
     */
    @Transactional(readOnly = true)
    public Optional<AppointmentTaskResponse> getTaskById(Long taskId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return taskRepository.findById(taskId)
            .filter(task -> task.getService().getMerchant().getId().equals(merchantProfile.getId()))
            .map(this::mapToResponse);
    }

    /**
     * Get a task by ID (public access for signed links).
     *
     * <p>Only returns active tasks.</p>
     *
     * @param taskId the ID of the task
     * @return Optional containing AppointmentTaskResponse if found and active
     */
    @Transactional(readOnly = true)
    public Optional<AppointmentTaskResponse> getTaskByIdPublic(Long taskId) {
        return taskRepository.findByIdAndActive(taskId, true)
            .map(this::mapToResponse);
    }

    /**
     * Get all tasks for the current merchant.
     *
     * <p>Returns both active and inactive tasks.</p>
     *
     * @return list of AppointmentTaskResponse
     */
    @Transactional(readOnly = true)
    public List<AppointmentTaskResponse> getAllTasks() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        // 获取商家的所有服务，然后获取每个服务的任务
        return serviceItemRepository.findByMerchant(merchantProfile).stream()
            .flatMap(service -> taskRepository.findByService(service).stream())
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get all active tasks for the current merchant.
     *
     * @return list of active AppointmentTaskResponse
     */
    @Transactional(readOnly = true)
    public List<AppointmentTaskResponse> getActiveTasks() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchantAndActiveTrue(merchantProfile).stream()
            .flatMap(service -> taskRepository.findByServiceAndActiveTrue(service).stream())
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get all inactive (soft-deleted) tasks for the current merchant.
     *
     * @return list of inactive AppointmentTaskResponse
     */
    @Transactional(readOnly = true)
    public List<AppointmentTaskResponse> getInactiveTasks() {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchant(merchantProfile).stream()
            .flatMap(service -> taskRepository.findByService(service).stream())
            .filter(task -> !task.isActive())
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get tasks for a specific service (for current merchant).
     *
     * @param serviceId the service ID
     * @return list of tasks for the service
     */
    @Transactional(readOnly = true)
    public List<AppointmentTaskResponse> getTasksByService(Long serviceId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        ServiceItem service = serviceItemRepository.findByIdAndMerchant(serviceId, merchantProfile)
            .orElseThrow(() -> new IllegalArgumentException(
                "Service not found or not owned by current merchant"));

        return taskRepository.findByService(service).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get tasks for a date range (for current merchant).
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of tasks in the date range
     */
    @Transactional(readOnly = true)
    public List<AppointmentTaskResponse> getTasksByDateRange(LocalDate startDate, LocalDate endDate) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        return serviceItemRepository.findByMerchantAndActiveTrue(merchantProfile).stream()
            .flatMap(service -> taskRepository.findByServiceAndTaskDateBetweenAndActiveTrue(
                service, startDate, endDate).stream())
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Count tasks for the current merchant.
     *
     * @param activeOnly if true, only count active tasks
     * @return count of tasks
     */
    @Transactional(readOnly = true)
    public long countTasks(boolean activeOnly) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        long total = serviceItemRepository.findByMerchant(merchantProfile).stream()
            .mapToLong(taskRepository::countByService)
            .sum();

        if (!activeOnly) {
            return total;
        }

        return serviceItemRepository.findByMerchantAndActiveTrue(merchantProfile).stream()
            .mapToLong(taskRepository::countByServiceAndActiveTrue)
            .sum();
    }

    // ============================================
    // 时段CRUD操作
    // ============================================

    /**
     * Create a new time slot for a task.
     *
     * <p>Only the merchant who owns the task can create slots.</p>
     *
     * @param taskId the task ID
     * @param request the slot creation request
     * @return SlotResponse containing the created slot
     * @throws IllegalArgumentException if task not found or slot time conflicts
     */
    @Transactional
    public SlotResponse createSlot(Long taskId, AppointmentSlotRequest request) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 验证所有权
        if (!task.getService().getMerchant().getId().equals(merchantProfile.getId())) {
            throw new IllegalArgumentException("Task not found or not owned by current merchant");
        }

        // 验证时间范围
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // 检查重复的开始时间
        if (slotRepository.existsByTaskAndStartTime(task, request.getStartTime())) {
            throw new IllegalArgumentException(
                "A slot with this start time already exists for this task");
        }

        AppointmentSlot slot = new AppointmentSlot(
            task,
            request.getStartTime(),
            request.getEndTime(),
            request.getCapacity()
        );

        AppointmentSlot savedSlot = slotRepository.save(slot);
        log.info("Created slot {} for task {}: {} - {}",
            savedSlot.getId(), taskId, savedSlot.getStartTime(), savedSlot.getEndTime());

        return mapToSlotResponse(savedSlot);
    }

    /**
     * Create multiple slots for a task at once.
     *
     * @param taskId the task ID
     * @param requests list of slot creation requests
     * @return list of created SlotResponse
     */
    @Transactional
    public List<SlotResponse> createSlots(Long taskId, List<AppointmentSlotRequest> requests) {
        return requests.stream()
            .map(request -> createSlot(taskId, request))
            .toList();
    }

    /**
     * Delete a slot.
     *
     * <p>Only the merchant who owns the task can delete slots.
     * Cannot delete slots that have bookings.</p>
     *
     * @param taskId the task ID
     * @param slotId the slot ID
     * @throws IllegalArgumentException if slot not found or has bookings
     */
    @Transactional
    public void deleteSlot(Long taskId, Long slotId) {
        MerchantProfile merchantProfile = getCurrentMerchantProfileOrThrow();

        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 验证所有权
        if (!task.getService().getMerchant().getId().equals(merchantProfile.getId())) {
            throw new IllegalArgumentException("Task not found or not owned by current merchant");
        }

        AppointmentSlot slot = slotRepository.findByIdAndTaskId(slotId, taskId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found in the specified task"));

        // 检查时段是否有预约
        if (slot.getBookedCount() > 0) {
            throw new IllegalArgumentException("Cannot delete slot with existing bookings");
        }

        slotRepository.delete(slot);
        log.info("Deleted slot {} from task {}", slotId, taskId);
    }

    // ============================================
    // 时段查询操作
    // ============================================

    /**
     * Get all slots for a task (for merchants).
     *
     * @param taskId the task ID
     * @return list of slots
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlots(Long taskId) {
        return slotRepository.findByTaskIdOrderByStartTimeAsc(taskId).stream()
            .map(this::mapToSlotResponse)
            .toList();
    }

    /**
     * Get all slots for a task (public access).
     *
     * @param taskId the task ID
     * @return list of slots
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlotsPublic(Long taskId) {
        // 验证任务存在且有效
        if (!taskRepository.findByIdAndActive(taskId, true).isPresent()) {
            throw new IllegalArgumentException("Task not found or not active");
        }
        return getSlots(taskId);
    }

    /**
     * Get only slots with available capacity for a task.
     *
     * @param taskId the task ID
     * @return list of slots with capacity
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlotsWithCapacity(Long taskId) {
        // 验证任务存在且有效
        if (!taskRepository.findByIdAndActive(taskId, true).isPresent()) {
            throw new IllegalArgumentException("Task not found or not active");
        }
        return slotRepository.findAvailableSlotsByTaskId(taskId).stream()
            .map(this::mapToSlotResponse)
            .toList();
    }

    // ============================================
    // 辅助方法
    // ============================================

    /**
     * Get the current authenticated user's merchant profile.
     *
     * @return Optional containing the MerchantProfile
     */
    private Optional<MerchantProfile> getCurrentMerchantProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return merchantProfileRepository.findByUserId(userDetails.getId());
        }

        return Optional.empty();
    }

    /**
     * Get the current authenticated user's merchant profile or throw an exception.
     *
     * @return the MerchantProfile
     * @throws IllegalStateException if no authenticated user or no merchant profile
     */
    private MerchantProfile getCurrentMerchantProfileOrThrow() {
        return getCurrentMerchantProfile()
            .orElseThrow(() -> new IllegalStateException(
                "No merchant profile found for current user. Please create a merchant profile first."));
    }

    /**
     * Map AppointmentTask entity to response DTO.
     *
     * @param task the entity to map
     * @return the response DTO
     */
    private AppointmentTaskResponse mapToResponse(AppointmentTask task) {
        ServiceItem service = task.getService();
        MerchantProfile merchant = service.getMerchant();

        // 获取时段统计数据
        int slotCount = (int) slotRepository.countByTask(task);
        int totalSlotCapacity = slotRepository.sumCapacityByTaskId(task.getId());
        int totalBookedCount = slotRepository.sumBookedCountByTaskId(task.getId());

        return AppointmentTaskResponse.builder()
            .id(task.getId())
            .serviceId(service.getId())
            .serviceName(service.getName())
            .merchantId(merchant.getId())
            .merchantBusinessName(merchant.getBusinessName())
            .title(task.getTitle())
            .description(task.getDescription())
            .taskDate(task.getTaskDate())
            .totalCapacity(task.getTotalCapacity())
            .slotCount(slotCount)
            .totalSlotCapacity(totalSlotCapacity)
            .totalBookedCount(totalBookedCount)
            .active(task.getActive())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }

    /**
     * Map AppointmentSlot entity to response DTO.
     *
     * @param slot the entity to map
     * @return the response DTO
     */
    private SlotResponse mapToSlotResponse(AppointmentSlot slot) {
        return SlotResponse.builder()
            .id(slot.getId())
            .startTime(slot.getStartTime())
            .endTime(slot.getEndTime())
            .capacity(slot.getCapacity())
            .bookedCount(slot.getBookedCount())
            .availableCount(slot.getAvailableCapacity())
            .hasCapacity(slot.hasAvailableCapacity())
            .build();
    }
}
