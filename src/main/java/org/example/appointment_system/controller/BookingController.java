package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.BookingRequest;
import org.example.appointment_system.dto.response.BookingResponse;
import org.example.appointment_system.dto.response.SlotResponse;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.service.BookingService;
import org.example.appointment_system.service.MerchantService;
import org.example.appointment_system.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约操作的REST控制器。
 *
 * <p>提供以下接口：</p>
 * <ul>
 *   <li>用户预约创建和管理</li>
 *   <li>商家预约管理</li>
 *   <li>查看可用时间段</li>
 *   <li>预约状态更新</li>
 * </ul>
 *
 * <h3>通知服务：</h3>
 * <p>当预约被创建或更新时，通过NotificationService记录日志。
 * 用户刷新页面可查看最新预约状态。</p>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;
    private final MerchantService merchantService;
    private final NotificationService notificationService;

    // ============================================
    // 用户预约接口
    // ============================================

    /**
     * 创建新预约。
     *
     * <p>为当前已认证用户在指定时段创建预约。
     * 使用乐观锁防止超售。</p>
     *
     * <p>预约成功后，向商家发送通知。</p>
     *
     * @param request 包含slotId和可选备注的预约请求
     * @return 包含已创建预约详情的BookingResponse
     */
    @PostMapping("/api/bookings")
    @Operation(summary = "Create a booking", description = "Create a new booking for the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or slot fully booked"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "409", description = "Already have a booking for this slot")
    })
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        log.info("Creating booking for slot: {}", request.getSlotId());

        BookingResponse response = bookingService.createBooking(request);

        // 发送通知给商家
        notificationService.notifyNewBooking(response);

        log.info("Booking created successfully: id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取当前用户的所有预约。
     *
     * @param pageable 分页参数
     * @return 预约响应分页
     */
    @GetMapping("/api/bookings/my")
    @Operation(summary = "Get my bookings", description = "Get all bookings for the current authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("Getting bookings for current user");
        Page<BookingResponse> bookings = bookingService.getMyBookings(pageable);
        return ResponseEntity.ok(bookings);
    }

    /**
     * 获取当前用户的活动预约。
     *
     * @return 活动预约列表
     */
    @GetMapping("/api/bookings/my/active")
    @Operation(summary = "Get my active bookings", description = "Get active (pending/confirmed) bookings for current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<BookingResponse>> getMyActiveBookings() {
        log.debug("Getting active bookings for current user");
        List<BookingResponse> bookings = bookingService.getMyActiveBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * 按状态筛选获取当前用户的预约。
     *
     * @param status 要筛选的预约状态
     * @return 具有指定状态的预约列表
     */
    @GetMapping("/api/bookings/my/status/{status}")
    @Operation(summary = "Get my bookings by status", description = "Get bookings filtered by status for current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<List<BookingResponse>> getMyBookingsByStatus(
        @Parameter(description = "Booking status filter")
        @PathVariable BookingStatus status
    ) {
        log.debug("Getting bookings for current user with status: {}", status);
        List<BookingResponse> bookings = bookingService.getMyBookingsByStatus(status);
        return ResponseEntity.ok(bookings);
    }

    /**
     * 根据ID获取特定预约。
     *
     * @param id 预约ID
     * @return 预约详情
     */
    @GetMapping("/api/bookings/{id}")
    @Operation(summary = "Get booking by ID", description = "Get details of a specific booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> getBookingById(
        @Parameter(description = "Booking ID") @PathVariable Long id
    ) {
        log.debug("Getting booking: {}", id);
        return bookingService.getBookingById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 取消预约。
     *
     * <p>只有创建预约的用户可以取消它。
     * 取消时向商家发送通知。</p>
     *
     * @param id 要取消的预约ID
     * @return 更新后的预约详情
     */
    @DeleteMapping("/api/bookings/{id}")
    @Operation(summary = "Cancel booking", description = "Cancel a booking (user operation)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Booking cannot be cancelled"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> cancelBooking(
        @Parameter(description = "Booking ID to cancel") @PathVariable Long id
    ) {
        log.info("Cancelling booking: {}", id);

        BookingResponse response = bookingService.cancelBooking(id);

        // 发送通知给商家
        notificationService.notifyBookingCancelled(response);

        log.info("Booking cancelled: id={}", id);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 商家预约接口
    // ============================================

    /**
     * 获取当前商家的所有预约。
     *
     * @param pageable 分页参数
     * @return 预约响应分页
     */
    @GetMapping("/api/merchants/bookings")
    @Operation(summary = "Get merchant bookings", description = "Get all bookings for the current merchant's tasks")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized (not a merchant)")
    })
    public ResponseEntity<Page<BookingResponse>> getMerchantBookings(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("Getting bookings for current merchant");

        Long merchantId = getCurrentMerchantId();
        Page<BookingResponse> bookings = bookingService.getBookingsByMerchant(merchantId, pageable);
        return ResponseEntity.ok(bookings);
    }

    /**
     * 按状态筛选获取商家的预约。
     *
     * @param status 要筛选的预约状态
     * @return 具有指定状态的预约列表
     */
    @GetMapping("/api/merchants/bookings/status/{status}")
    @Operation(summary = "Get merchant bookings by status", description = "Get merchant bookings filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized (not a merchant)"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<List<BookingResponse>> getMerchantBookingsByStatus(
        @Parameter(description = "Booking status filter") @PathVariable BookingStatus status
    ) {
        log.debug("Getting bookings for current merchant with status: {}", status);

        Long merchantId = getCurrentMerchantId();
        List<BookingResponse> bookings = bookingService.getBookingsByMerchantAndStatus(merchantId, status);
        return ResponseEntity.ok(bookings);
    }

    /**
     * 确认预约（商家操作）。
     *
     * <p>将预约状态从PENDING更改为CONFIRMED。
     * 向用户发送通知。</p>
     *
     * @param id 要确认的预约ID
     * @return 更新后的预约详情
     */
    @PutMapping("/api/merchants/bookings/{id}/confirm")
    @Operation(summary = "Confirm booking", description = "Confirm a booking (merchant operation)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking confirmed successfully"),
        @ApiResponse(responseCode = "400", description = "Booking cannot be confirmed"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> confirmBooking(
        @Parameter(description = "Booking ID to confirm") @PathVariable Long id
    ) {
        log.info("Confirming booking: {}", id);

        Long merchantId = getCurrentMerchantId();
        BookingResponse response = bookingService.confirmBooking(id, merchantId);

        // 发送通知给用户
        notificationService.notifyBookingConfirmed(response);

        log.info("Booking confirmed: id={}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * 将预约标记为已完成（商家操作）。
     *
     * <p>将预约状态从CONFIRMED更改为COMPLETED。
     * 向用户发送通知。</p>
     *
     * @param id 要完成的预约ID
     * @return 更新后的预约详情
     */
    @PutMapping("/api/merchants/bookings/{id}/complete")
    @Operation(summary = "Complete booking", description = "Mark a booking as completed (merchant operation)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking completed successfully"),
        @ApiResponse(responseCode = "400", description = "Booking cannot be completed"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> completeBooking(
        @Parameter(description = "Booking ID to complete") @PathVariable Long id
    ) {
        log.info("Completing booking: {}", id);

        Long merchantId = getCurrentMerchantId();
        BookingResponse response = bookingService.completeBooking(id, merchantId);

        // 发送通知给用户
        notificationService.notifyBookingCompleted(response);

        log.info("Booking completed: id={}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * 取消预约（商家操作）。
     *
     * <p>商家可以取消自己任务上的预约。
     * 向用户发送通知。</p>
     *
     * @param id 要取消的预约ID
     * @return 更新后的预约详情
     */
    @DeleteMapping("/api/merchants/bookings/{id}")
    @Operation(summary = "Cancel booking (merchant)", description = "Cancel a booking as a merchant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Booking cannot be cancelled"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> cancelBookingByMerchant(
        @Parameter(description = "Booking ID to cancel") @PathVariable Long id
    ) {
        log.info("Merchant cancelling booking: {}", id);

        Long merchantId = getCurrentMerchantId();
        BookingResponse response = bookingService.cancelBookingByMerchant(id, merchantId);

        // 发送通知给用户
        notificationService.notifyBookingCancelled(response);

        log.info("Booking cancelled by merchant: id={}", id);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 时段查询接口
    // ============================================

    /**
     * 获取任务的可用时段。
     *
     * @param taskId 任务ID
     * @return 包含容量信息的可用时段列表
     */
    @GetMapping("/api/tasks/{taskId}/slots")
    @Operation(summary = "Get available slots", description = "Get all time slots for a task (public endpoint)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Slots retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<List<SlotResponse>> getAvailableSlots(
        @Parameter(description = "Task ID") @PathVariable Long taskId
    ) {
        log.debug("Getting available slots for task: {}", taskId);
        List<SlotResponse> slots = bookingService.getAvailableSlots(taskId);
        return ResponseEntity.ok(slots);
    }

    /**
     * 只获取有可用容量的时段。
     *
     * @param taskId 任务ID
     * @return 有可用容量的时段列表
     */
    @GetMapping("/api/tasks/{taskId}/slots/available")
    @Operation(summary = "Get slots with capacity", description = "Get only slots that have available capacity")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available slots retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<List<SlotResponse>> getSlotsWithCapacity(
        @Parameter(description = "Task ID") @PathVariable Long taskId
    ) {
        log.debug("Getting slots with capacity for task: {}", taskId);
        List<SlotResponse> slots = bookingService.getSlotsWithCapacity(taskId);
        return ResponseEntity.ok(slots);
    }

    /**
     * 根据ID获取特定时段。
     *
     * @param slotId 时段ID
     * @return 包含容量信息的时段详情
     */
    @GetMapping("/api/slots/{slotId}")
    @Operation(summary = "Get slot by ID", description = "Get details of a specific time slot")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Slot retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Slot not found")
    })
    public ResponseEntity<SlotResponse> getSlotById(
        @Parameter(description = "Slot ID") @PathVariable Long slotId
    ) {
        log.debug("Getting slot: {}", slotId);
        return bookingService.getSlotById(slotId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ============================================
    // 统计接口
    // ============================================

    /**
     * 统计当前用户的预约数量。
     *
     * @return 用户预约总数
     */
    @GetMapping("/api/bookings/my/count")
    @Operation(summary = "Count my bookings", description = "Get total count of current user's bookings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Long> countMyBookings() {
        long count = bookingService.countMyBookings();
        return ResponseEntity.ok(count);
    }

    /**
     * 统计当前用户的活动预约数量。
     *
     * @return 活动预约数量
     */
    @GetMapping("/api/bookings/my/count/active")
    @Operation(summary = "Count my active bookings", description = "Get count of active bookings for current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Long> countMyActiveBookings() {
        long count = bookingService.countMyActiveBookings();
        return ResponseEntity.ok(count);
    }

    /**
     * 检查当前用户是否在某时段有活动预约。
     *
     * @param slotId 要检查的时段ID
     * @return 如果用户在此时段有活动预约返回true
     */
    @GetMapping("/api/bookings/my/has-booking/{slotId}")
    @Operation(summary = "Check booking exists", description = "Check if current user has an active booking for a slot")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Check completed"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Boolean> hasActiveBookingForSlot(
        @Parameter(description = "Slot ID to check") @PathVariable Long slotId
    ) {
        boolean hasBooking = bookingService.hasActiveBookingForSlot(slotId);
        return ResponseEntity.ok(hasBooking);
    }

    // ============================================
    // 辅助方法
    // ============================================

    /**
     * 获取当前商家的资料ID。
     *
     * @return 商家资料ID
     * @throws IllegalStateException 如果用户不是商家或没有资料
     */
    private Long getCurrentMerchantId() {
        return merchantService.getCurrentMerchantProfile()
            .orElseThrow(() -> new IllegalStateException("Merchant profile not found for current user"))
            .getId();
    }
}
