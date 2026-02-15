package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.UserRepository;
import org.example.appointment_system.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for booking management operations.
 *
 * <p>Handles booking operations including:</p>
 * <ul>
 *   <li>Creating bookings with optimistic locking for concurrency control</li>
 *   <li>Querying available time slots</li>
 *   <li>Cancelling bookings</li>
 *   <li>Updating booking status</li>
 *   <li>Retrieving user and merchant bookings</li>
 * </ul>
 *
 * <h3>Concurrency Control:</h3>
 * <p>This service uses optimistic locking via the @Version annotation on the Booking entity.
 * When concurrent booking attempts occur, only one will succeed. The others will receive
 * an ObjectOptimisticLockingFailureException.</p>
 *
 * <h3>Capacity Management:</h3>
 * <p>The service also manages slot capacity by atomically incrementing/decrementing
 * the bookedCount on slots when bookings are created/cancelled.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    // ============================================
    // Create Booking
    // ============================================

    /**
     * Create a new booking for the current user.
     *
     * <p>This method uses optimistic locking to handle concurrent booking attempts.
     * If the slot is already fully booked, or if the user has already booked this slot,
     * an exception will be thrown.</p>
     *
     * <p>The booking process:</p>
     * <ol>
     *   <li>Verify the slot exists and has capacity</li>
     *   <li>Check for duplicate bookings (same user + slot)</li>
     *   <li>Increment slot's bookedCount atomically</li>
     *   <li>Create and save the booking</li>
     * </ol>
     *
     * @param request the booking request containing slotId and optional remark
     * @return BookingResponse containing the created booking details
     * @throws IllegalArgumentException if slot not found, no capacity, or duplicate booking
     * @throws ObjectOptimisticLockingFailureException if concurrent modification detected
     * @throws IllegalStateException if no authenticated user found
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        User currentUser = getCurrentUserOrThrow();

        // Fetch and validate the slot
        AppointmentSlot slot = slotRepository.findById(request.getSlotId())
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + request.getSlotId()));

        // Check if slot has capacity
        if (!slot.hasAvailableCapacity()) {
            log.warn("Slot {} is fully booked (capacity: {}, booked: {})",
                slot.getId(), slot.getCapacity(), slot.getBookedCount());
            throw new IllegalArgumentException("This time slot is fully booked");
        }

        // Check for duplicate booking (same user + slot)
        if (bookingRepository.existsActiveBookingByUserIdAndSlotId(currentUser.getId(), slot.getId())) {
            log.warn("User {} already has an active booking for slot {}", currentUser.getId(), slot.getId());
            throw new IllegalArgumentException("You already have an active booking for this time slot");
        }

        // Increment booked count atomically
        if (!slot.incrementBookedCount()) {
            log.warn("Failed to increment booked count for slot {} - may be full", slot.getId());
            throw new IllegalArgumentException("This time slot is fully booked");
        }
        slotRepository.save(slot);

        // Create the booking
        Booking booking = new Booking(currentUser, slot, request.getRemark());
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Created booking {} for user {} on slot {}",
            savedBooking.getId(), currentUser.getId(), slot.getId());

        return mapToResponse(savedBooking);
    }

    /**
     * Create a booking for a specific user (used by merchants/admins or signed links).
     *
     * <p>This overload allows creating bookings for other users, useful for
     * merchant-assisted bookings or signed link access.</p>
     *
     * @param userId the ID of the user to book for
     * @param request the booking request
     * @return BookingResponse containing the created booking details
     */
    @Transactional
    public BookingResponse createBookingForUser(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        AppointmentSlot slot = slotRepository.findById(request.getSlotId())
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + request.getSlotId()));

        if (!slot.hasAvailableCapacity()) {
            throw new IllegalArgumentException("This time slot is fully booked");
        }

        if (bookingRepository.existsActiveBookingByUserIdAndSlotId(userId, slot.getId())) {
            throw new IllegalArgumentException("User already has an active booking for this time slot");
        }

        slot.incrementBookedCount();
        slotRepository.save(slot);

        Booking booking = new Booking(user, slot, request.getRemark());
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Created booking {} for user {} on slot {} (created by merchant/admin)",
            savedBooking.getId(), userId, slot.getId());

        return mapToResponse(savedBooking);
    }

    // ============================================
    // Cancel Booking
    // ============================================

    /**
     * Cancel a booking.
     *
     * <p>Only the user who made the booking can cancel it.
     * Cancelling decrements the slot's bookedCount.</p>
     *
     * @param bookingId the ID of the booking to cancel
     * @return BookingResponse containing the updated booking details
     * @throws IllegalArgumentException if booking not found or cannot be cancelled
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        User currentUser = getCurrentUserOrThrow();

        Booking booking = bookingRepository.findByIdAndUserId(bookingId, currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Booking not found with id: " + bookingId + " for current user"));

        return cancelBookingInternal(booking);
    }

    /**
     * Cancel a booking by merchant (for any user's booking on merchant's tasks).
     *
     * <p>Merchants can cancel bookings on their own tasks.</p>
     *
     * @param bookingId the ID of the booking to cancel
     * @param merchantId the merchant's ID
     * @return BookingResponse containing the updated booking details
     */
    @Transactional
    public BookingResponse cancelBookingByMerchant(Long bookingId, Long merchantId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify the booking belongs to the merchant's task
        Long bookingMerchantId = getMerchantIdForBooking(booking);
        if (!merchantId.equals(bookingMerchantId)) {
            throw new IllegalArgumentException("Booking does not belong to this merchant");
        }

        return cancelBookingInternal(booking);
    }

    /**
     * Internal method to handle cancellation logic.
     */
    private BookingResponse cancelBookingInternal(Booking booking) {
        if (!booking.canCancel()) {
            log.warn("Booking {} cannot be cancelled - current status: {}", booking.getId(), booking.getStatus());
            throw new IllegalArgumentException(
                "Booking cannot be cancelled. Current status: " + booking.getStatus().getDisplayName());
        }

        // Cancel the booking
        booking.cancel();
        Booking savedBooking = bookingRepository.save(booking);

        // Decrement slot's booked count
        AppointmentSlot slot = booking.getSlot();
        slot.decrementBookedCount();
        slotRepository.save(slot);

        log.info("Cancelled booking {}, status: {}", savedBooking.getId(), savedBooking.getStatus());

        return mapToResponse(savedBooking);
    }

    // ============================================
    // Update Booking Status
    // ============================================

    /**
     * Confirm a booking (merchant operation).
     *
     * <p>Only merchants can confirm bookings for their tasks.</p>
     *
     * @param bookingId the ID of the booking to confirm
     * @param merchantId the merchant's ID
     * @return BookingResponse containing the updated booking details
     */
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, Long merchantId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify ownership
        Long bookingMerchantId = getMerchantIdForBooking(booking);
        if (!merchantId.equals(bookingMerchantId)) {
            throw new IllegalArgumentException("Booking does not belong to this merchant");
        }

        if (!booking.confirm()) {
            throw new IllegalArgumentException(
                "Booking cannot be confirmed. Current status: " + booking.getStatus().getDisplayName());
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Confirmed booking {} by merchant {}", bookingId, merchantId);

        return mapToResponse(savedBooking);
    }

    /**
     * Mark a booking as completed (merchant operation).
     *
     * @param bookingId the ID of the booking to complete
     * @param merchantId the merchant's ID
     * @return BookingResponse containing the updated booking details
     */
    @Transactional
    public BookingResponse completeBooking(Long bookingId, Long merchantId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify ownership
        Long bookingMerchantId = getMerchantIdForBooking(booking);
        if (!merchantId.equals(bookingMerchantId)) {
            throw new IllegalArgumentException("Booking does not belong to this merchant");
        }

        if (!booking.complete()) {
            throw new IllegalArgumentException(
                "Booking cannot be completed. Current status: " + booking.getStatus().getDisplayName());
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Completed booking {} by merchant {}", bookingId, merchantId);

        return mapToResponse(savedBooking);
    }

    // ============================================
    // Query Bookings
    // ============================================

    /**
     * Get a booking by ID for the current user.
     *
     * @param bookingId the booking ID
     * @return Optional containing BookingResponse if found and belongs to user
     */
    @Transactional(readOnly = true)
    public Optional<BookingResponse> getBookingById(Long bookingId) {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.findByIdAndUserId(bookingId, currentUser.getId())
            .map(this::mapToResponse);
    }

    /**
     * Get all bookings for the current user.
     *
     * @return list of BookingResponse for user's bookings
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.findByUserId(currentUser.getId()).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get bookings for the current user with pagination.
     *
     * @param pageable pagination information
     * @return page of BookingResponse
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.findByUserId(currentUser.getId(), pageable)
            .map(this::mapToResponse);
    }

    /**
     * Get bookings for the current user filtered by status.
     *
     * @param status the booking status to filter by
     * @return list of BookingResponse
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookingsByStatus(BookingStatus status) {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.findByUserIdAndStatus(currentUser.getId(), status).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get active bookings for the current user.
     *
     * @return list of active BookingResponse
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyActiveBookings() {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.findByUserIdAndStatusIn(
            currentUser.getId(),
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        ).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all bookings for a merchant's tasks.
     *
     * @param merchantId the merchant ID
     * @return list of BookingResponse
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByMerchant(Long merchantId) {
        return bookingRepository.findByMerchantId(merchantId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get bookings for a merchant's tasks with pagination.
     *
     * @param merchantId the merchant ID
     * @param pageable pagination information
     * @return page of BookingResponse
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByMerchant(Long merchantId, Pageable pageable) {
        return bookingRepository.findByMerchantId(merchantId, pageable)
            .map(this::mapToResponse);
    }

    /**
     * Get bookings for a merchant filtered by status.
     *
     * @param merchantId the merchant ID
     * @param status the booking status
     * @return list of BookingResponse
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByMerchantAndStatus(Long merchantId, BookingStatus status) {
        return bookingRepository.findByMerchantIdAndStatus(merchantId, status).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all bookings for a specific task.
     *
     * @param taskId the task ID
     * @return list of BookingResponse
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByTask(Long taskId) {
        return bookingRepository.findByTaskId(taskId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get bookings for a task with pagination.
     *
     * @param taskId the task ID
     * @param pageable pagination information
     * @return page of BookingResponse
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByTask(Long taskId, Pageable pageable) {
        return bookingRepository.findByTaskId(taskId, pageable)
            .map(this::mapToResponse);
    }

    // ============================================
    // Query Available Slots
    // ============================================

    /**
     * Get available slots for a task.
     *
     * @param taskId the task ID
     * @return list of SlotResponse with availability information
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(Long taskId) {
        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

        return slotRepository.findByTaskOrderByStartTimeAsc(task).stream()
            .map(this::mapSlotToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get only slots that have available capacity.
     *
     * @param taskId the task ID
     * @return list of SlotResponse for available slots only
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlotsWithCapacity(Long taskId) {
        AppointmentTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

        return slotRepository.findAvailableSlotsByTask(task).stream()
            .map(this::mapSlotToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific slot with availability information.
     *
     * @param slotId the slot ID
     * @return Optional containing SlotResponse
     */
    @Transactional(readOnly = true)
    public Optional<SlotResponse> getSlotById(Long slotId) {
        return slotRepository.findById(slotId)
            .map(this::mapSlotToResponse);
    }

    // ============================================
    // Count Operations
    // ============================================

    /**
     * Count total bookings for the current user.
     *
     * @return count of bookings
     */
    @Transactional(readOnly = true)
    public long countMyBookings() {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.countByUserId(currentUser.getId());
    }

    /**
     * Count active bookings for the current user.
     *
     * @return count of active bookings
     */
    @Transactional(readOnly = true)
    public long countMyActiveBookings() {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.countByUserIdAndStatus(currentUser.getId(), BookingStatus.PENDING)
             + bookingRepository.countByUserIdAndStatus(currentUser.getId(), BookingStatus.CONFIRMED);
    }

    /**
     * Count bookings for a merchant.
     *
     * @param merchantId the merchant ID
     * @return count of bookings
     */
    @Transactional(readOnly = true)
    public long countBookingsByMerchant(Long merchantId) {
        return bookingRepository.countByMerchantId(merchantId);
    }

    /**
     * Count active bookings for a task.
     *
     * @param taskId the task ID
     * @return count of active bookings
     */
    @Transactional(readOnly = true)
    public long countActiveBookingsByTask(Long taskId) {
        return bookingRepository.countActiveByTaskId(taskId);
    }

    // ============================================
    // Check Operations
    // ============================================

    /**
     * Check if current user has an active booking for a slot.
     *
     * @param slotId the slot ID
     * @return true if user has active booking
     */
    @Transactional(readOnly = true)
    public boolean hasActiveBookingForSlot(Long slotId) {
        User currentUser = getCurrentUserOrThrow();
        return bookingRepository.existsActiveBookingByUserIdAndSlotId(currentUser.getId(), slotId);
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Get the current authenticated user.
     *
     * @return Optional containing the User
     */
    private Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findById(userDetails.getId());
        }

        return Optional.empty();
    }

    /**
     * Get the current authenticated user or throw an exception.
     *
     * @return the User
     * @throws IllegalStateException if no authenticated user found
     */
    private User getCurrentUserOrThrow() {
        return getCurrentUser()
            .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Get the merchant ID for a booking through the slot -> task -> service -> merchant chain.
     *
     * @param booking the booking
     * @return the merchant ID, or null if not found
     */
    private Long getMerchantIdForBooking(Booking booking) {
        AppointmentSlot slot = booking.getSlot();
        if (slot == null) return null;

        AppointmentTask task = slot.getTask();
        if (task == null) return null;

        ServiceItem service = task.getService();
        if (service == null) return null;

        MerchantProfile merchant = service.getMerchant();
        return merchant != null ? merchant.getId() : null;
    }

    /**
     * Map Booking entity to response DTO.
     *
     * @param booking the entity to map
     * @return the response DTO
     */
    private BookingResponse mapToResponse(Booking booking) {
        AppointmentSlot slot = booking.getSlot();
        AppointmentTask task = slot != null ? slot.getTask() : null;
        ServiceItem service = task != null ? task.getService() : null;
        MerchantProfile merchant = service != null ? service.getMerchant() : null;
        User user = booking.getUser();

        return BookingResponse.builder()
            .id(booking.getId())
            .userId(user != null ? user.getId() : null)
            .username(user != null ? user.getUsername() : null)
            .slotId(slot != null ? slot.getId() : null)
            .taskId(task != null ? task.getId() : null)
            .taskTitle(task != null ? task.getTitle() : null)
            .taskDate(task != null ? task.getTaskDate() : null)
            .startTime(slot != null ? slot.getStartTime() : null)
            .endTime(slot != null ? slot.getEndTime() : null)
            .serviceId(service != null ? service.getId() : null)
            .serviceName(service != null ? service.getName() : null)
            .merchantId(merchant != null ? merchant.getId() : null)
            .merchantBusinessName(merchant != null ? merchant.getBusinessName() : null)
            .status(booking.getStatus())
            .statusDisplayName(booking.getStatus().getDisplayName())
            .remark(booking.getRemark())
            .version(booking.getVersion())
            .createdAt(booking.getCreatedAt())
            .updatedAt(booking.getUpdatedAt())
            .build();
    }

    /**
     * Map AppointmentSlot entity to SlotResponse DTO.
     *
     * @param slot the entity to map
     * @return the response DTO
     */
    private SlotResponse mapSlotToResponse(AppointmentSlot slot) {
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
