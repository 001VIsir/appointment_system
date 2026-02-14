package org.example.appointment_system.repository;

import org.example.appointment_system.entity.*;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BookingRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private User testUser;
    private User anotherUser;
    private User merchantUser;
    private MerchantProfile merchant;
    private ServiceItem testService;
    private AppointmentTask testTask;
    private AppointmentSlot morningSlot;
    private AppointmentSlot afternoonSlot;

    @BeforeEach
    void setUp() {
        // Create users
        testUser = new User("testuser", "password", "test@example.com", UserRole.USER);
        anotherUser = new User("anotheruser", "password", "another@example.com", UserRole.USER);
        merchantUser = new User("merchant1", "password", "merchant@example.com", UserRole.MERCHANT);
        entityManager.persist(testUser);
        entityManager.persist(anotherUser);
        entityManager.persist(merchantUser);

        // Create merchant profile
        merchant = new MerchantProfile(merchantUser, "Test Business");
        entityManager.persist(merchant);

        // Create service item
        testService = new ServiceItem(
            merchant,
            "Haircut",
            "Professional haircut service",
            ServiceCategory.BEAUTY,
            30,
            new BigDecimal("25.00"),
            true
        );
        entityManager.persist(testService);
        entityManager.flush();

        // Create appointment task
        testTask = new AppointmentTask(
            testService,
            "Morning Appointments",
            LocalDate.of(2026, 2, 20),
            10
        );
        entityManager.persist(testTask);
        entityManager.flush();

        // Create appointment slots
        morningSlot = new AppointmentSlot(
            testTask,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            5,
            0
        );
        afternoonSlot = new AppointmentSlot(
            testTask,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            3,
            0
        );
        entityManager.persist(morningSlot);
        entityManager.persist(afternoonSlot);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and find booking by ID")
        void shouldSaveAndFindById() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot, "Test remark");

            // Act
            Booking savedBooking = bookingRepository.save(booking);
            Optional<Booking> foundBooking = bookingRepository.findById(savedBooking.getId());

            // Assert
            assertThat(foundBooking).isPresent();
            assertThat(foundBooking.get().getUser()).isEqualTo(testUser);
            assertThat(foundBooking.get().getSlot()).isEqualTo(morningSlot);
            assertThat(foundBooking.get().getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(foundBooking.get().getRemark()).isEqualTo("Test remark");
        }

        @Test
        @DisplayName("Should update booking")
        void shouldUpdateBooking() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            Booking savedBooking = bookingRepository.save(booking);
            entityManager.flush();

            // Act
            savedBooking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(savedBooking);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<Booking> foundBooking = bookingRepository.findById(savedBooking.getId());
            assertThat(foundBooking).isPresent();
            assertThat(foundBooking.get().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should delete booking")
        void shouldDeleteBooking() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            Booking savedBooking = bookingRepository.save(booking);
            entityManager.flush();

            // Act
            bookingRepository.delete(savedBooking);
            entityManager.flush();

            // Assert
            Optional<Booking> deletedBooking = bookingRepository.findById(savedBooking.getId());
            assertThat(deletedBooking).isEmpty();
        }

        @Test
        @DisplayName("Should find all bookings")
        void shouldFindAll() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            entityManager.flush();

            // Act
            List<Booking> bookings = bookingRepository.findAll();

            // Assert
            assertThat(bookings).hasSize(3);
        }

        @Test
        @DisplayName("Should count bookings")
        void shouldCount() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            entityManager.flush();

            // Act
            long count = bookingRepository.count();

            // Assert
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Find by User")
    class FindByUserTests {

        @Test
        @DisplayName("Should find bookings by user")
        void shouldFindByUser() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            entityManager.flush();

            // Act
            List<Booking> userBookings = bookingRepository.findByUser(testUser);

            // Assert
            assertThat(userBookings).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by user ID")
        void shouldFindByUserId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            List<Booking> userBookings = bookingRepository.findByUserId(testUser.getId());

            // Assert
            assertThat(userBookings).hasSize(1);
            assertThat(userBookings.get(0).getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should find bookings by user ID with pagination")
        void shouldFindByUserIdWithPagination() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            entityManager.flush();

            // Act
            Page<Booking> page = bookingRepository.findByUserId(testUser.getId(), PageRequest.of(0, 10));

            // Assert
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find bookings by user and status")
        void shouldFindByUserAndStatus() {
            // Arrange
            Booking pendingBooking = new Booking(testUser, morningSlot);
            Booking confirmedBooking = new Booking(testUser, afternoonSlot);
            confirmedBooking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pendingBooking);
            bookingRepository.save(confirmedBooking);
            entityManager.flush();

            // Act
            List<Booking> pendingBookings = bookingRepository.findByUserAndStatus(testUser, BookingStatus.PENDING);
            List<Booking> confirmedBookings = bookingRepository.findByUserAndStatus(testUser, BookingStatus.CONFIRMED);

            // Assert
            assertThat(pendingBookings).hasSize(1);
            assertThat(confirmedBookings).hasSize(1);
        }

        @Test
        @DisplayName("Should find bookings by user ID and status")
        void shouldFindByUserIdAndStatus() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            entityManager.flush();

            // Act
            List<Booking> found = bookingRepository.findByUserIdAndStatus(testUser.getId(), BookingStatus.CONFIRMED);
            List<Booking> notFound = bookingRepository.findByUserIdAndStatus(testUser.getId(), BookingStatus.PENDING);

            // Assert
            assertThat(found).hasSize(1);
            assertThat(notFound).isEmpty();
        }

        @Test
        @DisplayName("Should find bookings by user ID and status in list")
        void shouldFindByUserIdAndStatusIn() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(testUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            List<Booking> activeBookings = bookingRepository.findByUserIdAndStatusIn(
                testUser.getId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
            );

            // Assert
            assertThat(activeBookings).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list for user with no bookings")
        void shouldReturnEmptyForUserWithNoBookings() {
            // Act
            List<Booking> bookings = bookingRepository.findByUser(anotherUser);

            // Assert
            assertThat(bookings).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by Slot")
    class FindBySlotTests {

        @Test
        @DisplayName("Should find bookings by slot")
        void shouldFindBySlot() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            entityManager.flush();

            // Act
            List<Booking> slotBookings = bookingRepository.findBySlot(morningSlot);

            // Assert
            assertThat(slotBookings).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by slot ID")
        void shouldFindBySlotId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            entityManager.flush();

            // Act
            List<Booking> slotBookings = bookingRepository.findBySlotId(morningSlot.getId());

            // Assert
            assertThat(slotBookings).hasSize(1);
        }

        @Test
        @DisplayName("Should find bookings by slot and status")
        void shouldFindBySlotAndStatus() {
            // Arrange
            Booking confirmed = new Booking(testUser, morningSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            Booking cancelled = new Booking(anotherUser, morningSlot);
            cancelled.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(confirmed);
            bookingRepository.save(cancelled);
            entityManager.flush();

            // Act
            List<Booking> confirmedBookings = bookingRepository.findBySlotAndStatus(morningSlot, BookingStatus.CONFIRMED);

            // Assert
            assertThat(confirmedBookings).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Find by User and Slot")
    class FindByUserAndSlotTests {

        @Test
        @DisplayName("Should find booking by user and slot")
        void shouldFindByUserAndSlot() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot, "My booking");
            bookingRepository.save(booking);
            entityManager.flush();

            // Act
            Optional<Booking> found = bookingRepository.findByUserAndSlot(testUser, morningSlot);

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getRemark()).isEqualTo("My booking");
        }

        @Test
        @DisplayName("Should find booking by user ID and slot ID")
        void shouldFindByUserIdAndSlotId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            entityManager.flush();

            // Act
            Optional<Booking> found = bookingRepository.findByUserIdAndSlotId(testUser.getId(), morningSlot.getId());

            // Assert
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("Should return empty when booking not found")
        void shouldReturnEmptyWhenNotFound() {
            // Act
            Optional<Booking> found = bookingRepository.findByUserIdAndSlotId(testUser.getId(), morningSlot.getId());

            // Assert
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should check if booking exists by user ID and slot ID")
        void shouldCheckExistenceByUserIdAndSlotId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            entityManager.flush();

            // Act & Assert
            assertThat(bookingRepository.existsByUserIdAndSlotId(testUser.getId(), morningSlot.getId())).isTrue();
            assertThat(bookingRepository.existsByUserIdAndSlotId(anotherUser.getId(), morningSlot.getId())).isFalse();
        }

        @Test
        @DisplayName("Should check if active booking exists")
        void shouldCheckActiveBookingExists() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            pending.setStatus(BookingStatus.PENDING);
            bookingRepository.save(pending);
            entityManager.flush();

            // Act & Assert
            assertThat(bookingRepository.existsActiveBookingByUserIdAndSlotId(testUser.getId(), morningSlot.getId())).isTrue();
        }

        @Test
        @DisplayName("Should return false for cancelled booking when checking active")
        void shouldReturnFalseForCancelledWhenCheckingActive() {
            // Arrange
            Booking cancelled = new Booking(testUser, morningSlot);
            cancelled.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(cancelled);
            entityManager.flush();

            // Act & Assert
            assertThat(bookingRepository.existsActiveBookingByUserIdAndSlotId(testUser.getId(), morningSlot.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Find by Status")
    class FindByStatusTests {

        @Test
        @DisplayName("Should find bookings by status")
        void shouldFindByStatus() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(anotherUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);

            // Assert
            assertThat(pendingBookings).hasSize(1);
        }

        @Test
        @DisplayName("Should find bookings by status with pagination")
        void shouldFindByStatusWithPagination() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            Page<Booking> page = bookingRepository.findByStatus(BookingStatus.PENDING, PageRequest.of(0, 10));

            // Assert
            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by status in list")
        void shouldFindByStatusIn() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(anotherUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            Booking cancelled = new Booking(testUser, afternoonSlot);
            cancelled.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            bookingRepository.save(cancelled);
            entityManager.flush();

            // Act
            List<Booking> activeBookings = bookingRepository.findByStatusIn(
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
            );

            // Assert
            assertThat(activeBookings).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Find by Task")
    class FindByTaskTests {

        @Test
        @DisplayName("Should find bookings by task ID")
        void shouldFindByTaskId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            entityManager.flush();

            // Act
            List<Booking> taskBookings = bookingRepository.findByTaskId(testTask.getId());

            // Assert
            assertThat(taskBookings).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by task ID with pagination")
        void shouldFindByTaskIdWithPagination() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            Page<Booking> page = bookingRepository.findByTaskId(testTask.getId(), PageRequest.of(0, 10));

            // Assert
            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by task ID and status")
        void shouldFindByTaskIdAndStatus() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(anotherUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            List<Booking> pendingBookings = bookingRepository.findByTaskIdAndStatus(testTask.getId(), BookingStatus.PENDING);

            // Assert
            assertThat(pendingBookings).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Find by Merchant")
    class FindByMerchantTests {

        @Test
        @DisplayName("Should find bookings by merchant ID")
        void shouldFindByMerchantId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            List<Booking> merchantBookings = bookingRepository.findByMerchantId(merchant.getId());

            // Assert
            assertThat(merchantBookings).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by merchant ID with pagination")
        void shouldFindByMerchantIdWithPagination() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            Page<Booking> page = bookingRepository.findByMerchantId(merchant.getId(), PageRequest.of(0, 10));

            // Assert
            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings by merchant ID and status")
        void shouldFindByMerchantIdAndStatus() {
            // Arrange
            Booking confirmed = new Booking(testUser, morningSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            Booking pending = new Booking(anotherUser, afternoonSlot);
            bookingRepository.save(confirmed);
            bookingRepository.save(pending);
            entityManager.flush();

            // Act
            List<Booking> confirmedBookings = bookingRepository.findByMerchantIdAndStatus(
                merchant.getId(),
                BookingStatus.CONFIRMED
            );

            // Assert
            assertThat(confirmedBookings).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountTests {

        @Test
        @DisplayName("Should count bookings by user ID")
        void shouldCountByUserId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            entityManager.flush();

            // Act
            long count = bookingRepository.countByUserId(testUser.getId());

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count bookings by user ID and status")
        void shouldCountByUserIdAndStatus() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(testUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            long pendingCount = bookingRepository.countByUserIdAndStatus(testUser.getId(), BookingStatus.PENDING);
            long confirmedCount = bookingRepository.countByUserIdAndStatus(testUser.getId(), BookingStatus.CONFIRMED);

            // Assert
            assertThat(pendingCount).isEqualTo(1);
            assertThat(confirmedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count bookings by slot ID")
        void shouldCountBySlotId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            entityManager.flush();

            // Act
            long count = bookingRepository.countBySlotId(morningSlot.getId());

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count active bookings by slot ID")
        void shouldCountActiveBySlotId() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(anotherUser, morningSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            long count = bookingRepository.countActiveBySlotId(morningSlot.getId());

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not count cancelled bookings as active")
        void shouldNotCountCancelledAsActive() {
            // Arrange
            Booking cancelled = new Booking(testUser, morningSlot);
            cancelled.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(cancelled);
            entityManager.flush();

            // Act
            long activeCount = bookingRepository.countActiveBySlotId(morningSlot.getId());
            long totalCount = bookingRepository.countBySlotId(morningSlot.getId());

            // Assert
            assertThat(activeCount).isZero();
            assertThat(totalCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count bookings by task ID")
        void shouldCountByTaskId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            long count = bookingRepository.countByTaskId(testTask.getId());

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count active bookings by task ID")
        void shouldCountActiveByTaskId() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(anotherUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            long count = bookingRepository.countActiveByTaskId(testTask.getId());

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count bookings by merchant ID")
        void shouldCountByMerchantId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            long count = bookingRepository.countByMerchantId(merchant.getId());

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count bookings by status")
        void shouldCountByStatus() {
            // Arrange
            Booking pending = new Booking(testUser, morningSlot);
            Booking confirmed = new Booking(anotherUser, afternoonSlot);
            confirmed.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(pending);
            bookingRepository.save(confirmed);
            entityManager.flush();

            // Act
            long pendingCount = bookingRepository.countByStatus(BookingStatus.PENDING);
            long confirmedCount = bookingRepository.countByStatus(BookingStatus.CONFIRMED);

            // Assert
            assertThat(pendingCount).isEqualTo(1);
            assertThat(confirmedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Find by Date Range")
    class FindByDateRangeTests {

        @Test
        @DisplayName("Should find bookings created within date range")
        void shouldFindByCreatedAtBetween() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<Booking> bookings = bookingRepository.findByCreatedAtBetween(start, end);

            // Assert
            assertThat(bookings).hasSize(2);
        }

        @Test
        @DisplayName("Should find bookings for user within date range")
        void shouldFindByUserIdAndCreatedAtBetween() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<Booking> bookings = bookingRepository.findByUserIdAndCreatedAtBetween(
                testUser.getId(),
                start,
                end
            );

            // Assert
            assertThat(bookings).hasSize(1);
            assertThat(bookings.get(0).getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should find bookings for merchant within date range")
        void shouldFindByMerchantIdAndCreatedAtBetween() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, afternoonSlot));
            entityManager.flush();

            // Act
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<Booking> bookings = bookingRepository.findByMerchantIdAndCreatedAtBetween(
                merchant.getId(),
                start,
                end
            );

            // Assert
            assertThat(bookings).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Find by ID with checks")
    class FindByIdWithChecksTests {

        @Test
        @DisplayName("Should find booking by ID and user ID")
        void shouldFindByIdAndUserId() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();

            // Act
            Optional<Booking> found = bookingRepository.findByIdAndUserId(saved.getId(), testUser.getId());

            // Assert
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("Should return empty when booking belongs to different user")
        void shouldReturnEmptyWhenBelongsToDifferentUser() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();

            // Act
            Optional<Booking> found = bookingRepository.findByIdAndUserId(saved.getId(), anotherUser.getId());

            // Assert
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find booking by ID and slot ID")
        void shouldFindByIdAndSlotId() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();

            // Act
            Optional<Booking> found = bookingRepository.findByIdAndSlotId(saved.getId(), morningSlot.getId());

            // Assert
            assertThat(found).isPresent();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("Should delete bookings by user ID")
        @org.springframework.transaction.annotation.Transactional
        void shouldDeleteByUserId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            entityManager.flush();

            // Act
            bookingRepository.deleteByUserId(testUser.getId());
            entityManager.flush();

            // Assert
            assertThat(bookingRepository.findByUserId(testUser.getId())).isEmpty();
            assertThat(bookingRepository.findByUserId(anotherUser.getId())).hasSize(1);
        }

        @Test
        @DisplayName("Should delete bookings by slot ID")
        @org.springframework.transaction.annotation.Transactional
        void shouldDeleteBySlotId() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            bookingRepository.save(new Booking(anotherUser, morningSlot));
            bookingRepository.save(new Booking(testUser, afternoonSlot));
            entityManager.flush();

            // Act
            bookingRepository.deleteBySlotId(morningSlot.getId());
            entityManager.flush();

            // Assert
            assertThat(bookingRepository.findBySlotId(morningSlot.getId())).isEmpty();
            assertThat(bookingRepository.findBySlotId(afternoonSlot.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Unique Constraint Tests")
    class UniqueConstraintTests {

        @Test
        @DisplayName("Should prevent duplicate booking for same user and slot")
        void shouldPreventDuplicateBooking() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            entityManager.flush();

            // Act & Assert - The second save should fail due to unique constraint
            Booking duplicate = new Booking(testUser, morningSlot);
            assertThatThrownBy(() -> {
                bookingRepository.save(duplicate);
                entityManager.flush();
            }).isInstanceOf(Exception.class);  // Can be DataIntegrityViolationException or ConstraintViolationException
        }

        @Test
        @DisplayName("Should allow different users to book same slot")
        void shouldAllowDifferentUsersToBookSameSlot() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            entityManager.flush();

            // Act - Different user should be able to book the same slot
            Booking anotherBooking = new Booking(anotherUser, morningSlot);
            Booking saved = bookingRepository.save(anotherBooking);
            entityManager.flush();

            // Assert
            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should allow same user to book different slots")
        void shouldAllowSameUserToBookDifferentSlots() {
            // Arrange
            bookingRepository.save(new Booking(testUser, morningSlot));
            entityManager.flush();

            // Act - Same user should be able to book different slots
            Booking anotherBooking = new Booking(testUser, afternoonSlot);
            Booking saved = bookingRepository.save(anotherBooking);
            entityManager.flush();

            // Assert
            assertThat(saved.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Optimistic Locking Tests")
    class OptimisticLockingTests {

        @Test
        @DisplayName("Should increment version on update")
        void shouldIncrementVersionOnUpdate() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();
            Long initialVersion = saved.getVersion();

            // Act
            saved.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(saved);
            entityManager.flush();

            // Assert
            assertThat(saved.getVersion()).isGreaterThan(initialVersion);
        }

        @Test
        @DisplayName("Should have initial version of 0")
        void shouldHaveInitialVersionZero() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);

            // Act
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();

            // Assert
            assertThat(saved.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Timestamp Auto-Generation")
    class TimestampTests {

        @Test
        @DisplayName("Should set createdAt on persist")
        void shouldSetCreatedAtOnPersist() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);

            // Act
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();

            // Assert
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set updatedAt on persist")
        void shouldSetUpdatedAtOnPersist() {
            // Arrange
            Booking booking = new Booking(testUser, morningSlot);

            // Act
            Booking saved = bookingRepository.save(booking);
            entityManager.flush();

            // Assert
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        }
    }
}
