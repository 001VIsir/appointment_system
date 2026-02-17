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
 * REST Controller for booking operations.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>User booking creation and management</li>
 *   <li>Merchant booking management</li>
 *   <li>Viewing available time slots</li>
 *   <li>Booking status updates</li>
 * </ul>
 *
 * <h3>Real-time Notifications:</h3>
 * <p>When bookings are created or updated, WebSocket notifications are sent
 * to the relevant merchant via the NotificationService.</p>
 *
 * <h3>WebSocket Integration:</h3>
 * <p>Merchants can connect to /ws to receive real-time booking notifications.</p>
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
    // User Booking Endpoints
    // ============================================

    /**
     * Create a new booking.
     *
     * <p>Creates a booking for the current authenticated user on the specified time slot.
     * Uses optimistic locking to prevent overbooking.</p>
     *
     * <p>Upon successful booking, a WebSocket notification is sent to the merchant.</p>
     *
     * @param request the booking request containing slotId and optional remark
     * @return BookingResponse with created booking details
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

        // Send WebSocket notification to merchant
        notificationService.notifyNewBooking(response);

        log.info("Booking created successfully: id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all bookings for the current user.
     *
     * @param pageable pagination parameters
     * @return page of booking responses
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
     * Get active bookings for the current user.
     *
     * @return list of active bookings
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
     * Get bookings for the current user filtered by status.
     *
     * @param status the booking status to filter by
     * @return list of bookings with the specified status
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
     * Get a specific booking by ID.
     *
     * @param id the booking ID
     * @return booking details
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
     * Cancel a booking.
     *
     * <p>Only the user who created the booking can cancel it.
     * A WebSocket notification is sent to the merchant upon cancellation.</p>
     *
     * @param id the booking ID to cancel
     * @return updated booking details
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

        // Send WebSocket notification to merchant
        notificationService.notifyBookingCancelled(response);

        log.info("Booking cancelled: id={}", id);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // Merchant Booking Endpoints
    // ============================================

    /**
     * Get all bookings for the current merchant.
     *
     * @param pageable pagination parameters
     * @return page of booking responses
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
     * Get merchant bookings filtered by status.
     *
     * @param status the booking status to filter by
     * @return list of bookings with the specified status
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
     * Confirm a booking (merchant operation).
     *
     * <p>Changes the booking status from PENDING to CONFIRMED.
     * A WebSocket notification is sent to the user.</p>
     *
     * @param id the booking ID to confirm
     * @return updated booking details
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

        // Send WebSocket notification to user
        notificationService.notifyBookingConfirmed(response);

        log.info("Booking confirmed: id={}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a booking as completed (merchant operation).
     *
     * <p>Changes the booking status from CONFIRMED to COMPLETED.
     * A WebSocket notification is sent to the user.</p>
     *
     * @param id the booking ID to complete
     * @return updated booking details
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

        // Send WebSocket notification to user
        notificationService.notifyBookingCompleted(response);

        log.info("Booking completed: id={}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a booking (merchant operation).
     *
     * <p>Merchants can cancel bookings on their own tasks.
     * A WebSocket notification is sent to the user.</p>
     *
     * @param id the booking ID to cancel
     * @return updated booking details
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

        // Send WebSocket notification to user
        notificationService.notifyBookingCancelled(response);

        log.info("Booking cancelled by merchant: id={}", id);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // Slot Query Endpoints
    // ============================================

    /**
     * Get available slots for a task.
     *
     * @param taskId the task ID
     * @return list of available slots with capacity information
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
     * Get only slots with available capacity for a task.
     *
     * @param taskId the task ID
     * @return list of slots that have available capacity
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
     * Get a specific slot by ID.
     *
     * @param slotId the slot ID
     * @return slot details with capacity information
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
    // Statistics Endpoints
    // ============================================

    /**
     * Count bookings for the current user.
     *
     * @return total count of user's bookings
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
     * Count active bookings for the current user.
     *
     * @return count of active bookings
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
     * Check if current user has an active booking for a slot.
     *
     * @param slotId the slot ID to check
     * @return true if user has an active booking for the slot
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
    // Helper Methods
    // ============================================

    /**
     * Get the current merchant's profile ID.
     *
     * @return the merchant profile ID
     * @throws IllegalStateException if user is not a merchant or has no profile
     */
    private Long getCurrentMerchantId() {
        return merchantService.getCurrentMerchantProfile()
            .orElseThrow(() -> new IllegalStateException("Merchant profile not found for current user"))
            .getId();
    }
}
