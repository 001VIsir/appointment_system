package org.example.appointment_system.repository;

import org.example.appointment_system.entity.AppointmentSlot;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AppointmentSlotRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class AppointmentSlotRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    private AppointmentTask testTask;
    private AppointmentTask anotherTask;
    private AppointmentSlot morningSlot;
    private AppointmentSlot afternoonSlot;
    private AppointmentSlot fullSlot;

    @BeforeEach
    void setUp() {
        // Create merchant user and profile
        User merchantUser = new User("merchant1", "password", "merchant1@example.com", UserRole.MERCHANT);
        entityManager.persist(merchantUser);

        MerchantProfile merchant = new MerchantProfile(merchantUser, "Test Business");
        entityManager.persist(merchant);

        // Create service items
        ServiceItem testService = new ServiceItem(
            merchant,
            "Haircut",
            "Professional haircut service",
            ServiceCategory.BEAUTY,
            30,
            new BigDecimal("25.00"),
            true
        );
        ServiceItem anotherService = new ServiceItem(
            merchant,
            "Massage",
            "Relaxing massage",
            ServiceCategory.BEAUTY,
            60,
            new BigDecimal("80.00"),
            true
        );
        entityManager.persist(testService);
        entityManager.persist(anotherService);
        entityManager.flush();

        // Create appointment tasks
        testTask = new AppointmentTask(
            testService,
            "Morning Appointments",
            LocalDate.of(2026, 2, 20),
            10
        );
        anotherTask = new AppointmentTask(
            anotherService,
            "Afternoon Appointments",
            LocalDate.of(2026, 2, 20),
            15
        );
        entityManager.persist(testTask);
        entityManager.persist(anotherTask);
        entityManager.flush();

        // Create appointment slots
        morningSlot = new AppointmentSlot(
            testTask,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            5,
            2  // 2 booked, 3 available
        );
        afternoonSlot = new AppointmentSlot(
            testTask,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            10,
            0  // 0 booked, all available
        );
        fullSlot = new AppointmentSlot(
            testTask,
            LocalTime.of(16, 0),
            LocalTime.of(17, 0),
            3,
            3  // full, no available
        );

        entityManager.persist(morningSlot);
        entityManager.persist(afternoonSlot);
        entityManager.persist(fullSlot);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and find slot by ID")
        void shouldSaveAndFindById() {
            // Arrange
            AppointmentSlot newSlot = new AppointmentSlot(
                testTask,
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                8
            );

            // Act
            AppointmentSlot savedSlot = appointmentSlotRepository.save(newSlot);
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findById(savedSlot.getId());

            // Assert
            assertThat(foundSlot).isPresent();
            assertThat(foundSlot.get().getStartTime()).isEqualTo(LocalTime.of(11, 0));
        }

        @Test
        @DisplayName("Should update slot")
        void shouldUpdateSlot() {
            // Arrange
            morningSlot.setCapacity(10);

            // Act
            AppointmentSlot updatedSlot = appointmentSlotRepository.save(morningSlot);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findById(updatedSlot.getId());
            assertThat(foundSlot).isPresent();
            assertThat(foundSlot.get().getCapacity()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should delete slot")
        void shouldDeleteSlot() {
            // Act
            appointmentSlotRepository.delete(morningSlot);
            entityManager.flush();

            // Assert
            Optional<AppointmentSlot> deletedSlot = appointmentSlotRepository.findById(morningSlot.getId());
            assertThat(deletedSlot).isEmpty();
        }

        @Test
        @DisplayName("Should find all slots")
        void shouldFindAll() {
            // Act
            List<AppointmentSlot> slots = appointmentSlotRepository.findAll();

            // Assert
            assertThat(slots).hasSize(3);
        }

        @Test
        @DisplayName("Should count slots")
        void shouldCount() {
            // Act
            long count = appointmentSlotRepository.count();

            // Assert
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Find by Task")
    class FindByTaskTests {

        @Test
        @DisplayName("Should find all slots by task")
        void shouldFindAllByTask() {
            // Act
            List<AppointmentSlot> slots = appointmentSlotRepository.findByTask(testTask);

            // Assert
            assertThat(slots).hasSize(3);
        }

        @Test
        @DisplayName("Should find all slots by task ID")
        void shouldFindAllByTaskId() {
            // Act
            List<AppointmentSlot> slots = appointmentSlotRepository.findByTaskId(testTask.getId());

            // Assert
            assertThat(slots).hasSize(3);
        }

        @Test
        @DisplayName("Should return empty list for task with no slots")
        void shouldReturnEmptyForTaskWithNoSlots() {
            // Act
            List<AppointmentSlot> slots = appointmentSlotRepository.findByTask(anotherTask);

            // Assert
            assertThat(slots).isEmpty();
        }

        @Test
        @DisplayName("Should find slots by task ordered by start time")
        void shouldFindByTaskOrderedByStartTime() {
            // Act
            List<AppointmentSlot> slots = appointmentSlotRepository.findByTaskOrderByStartTimeAsc(testTask);

            // Assert
            assertThat(slots).hasSize(3);
            assertThat(slots.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(slots.get(1).getStartTime()).isEqualTo(LocalTime.of(14, 0));
            assertThat(slots.get(2).getStartTime()).isEqualTo(LocalTime.of(16, 0));
        }

        @Test
        @DisplayName("Should find slots by task ID ordered by start time")
        void shouldFindByTaskIdOrderedByStartTime() {
            // Act
            List<AppointmentSlot> slots = appointmentSlotRepository.findByTaskIdOrderByStartTimeAsc(testTask.getId());

            // Assert
            assertThat(slots).hasSize(3);
            assertThat(slots).isSortedAccordingTo((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));
        }
    }

    @Nested
    @DisplayName("Find by ID and Task")
    class FindByIdAndTaskTests {

        @Test
        @DisplayName("Should find slot by ID and task")
        void shouldFindByIdAndTask() {
            // Act
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findByIdAndTask(
                morningSlot.getId(),
                testTask
            );

            // Assert
            assertThat(foundSlot).isPresent();
            assertThat(foundSlot.get().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("Should return empty when slot belongs to different task")
        void shouldReturnEmptyWhenSlotBelongsToDifferentTask() {
            // Act
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findByIdAndTask(
                morningSlot.getId(),
                anotherTask
            );

            // Assert
            assertThat(foundSlot).isEmpty();
        }

        @Test
        @DisplayName("Should find slot by ID and task ID")
        void shouldFindByIdAndTaskId() {
            // Act
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findByIdAndTaskId(
                morningSlot.getId(),
                testTask.getId()
            );

            // Assert
            assertThat(foundSlot).isPresent();
            assertThat(foundSlot.get().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        }
    }

    @Nested
    @DisplayName("Find Available Slots")
    class FindAvailableSlotsTests {

        @Test
        @DisplayName("Should find available slots by task")
        void shouldFindAvailableSlotsByTask() {
            // Act
            List<AppointmentSlot> availableSlots = appointmentSlotRepository.findAvailableSlotsByTask(testTask);

            // Assert - morning and afternoon have capacity, fullSlot doesn't
            assertThat(availableSlots).hasSize(2);
            assertThat(availableSlots).extracting(AppointmentSlot::getStartTime)
                .containsExactlyInAnyOrder(LocalTime.of(9, 0), LocalTime.of(14, 0))
                .doesNotContain(LocalTime.of(16, 0));
        }

        @Test
        @DisplayName("Should find available slots by task ID")
        void shouldFindAvailableSlotsByTaskId() {
            // Act
            List<AppointmentSlot> availableSlots = appointmentSlotRepository.findAvailableSlotsByTaskId(testTask.getId());

            // Assert
            assertThat(availableSlots).hasSize(2);
        }

        @Test
        @DisplayName("Should count available slots")
        void shouldCountAvailableSlots() {
            // Act
            long count = appointmentSlotRepository.countAvailableSlotsByTask(testTask);

            // Assert
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Find by Time")
    class FindByTimeTests {

        @Test
        @DisplayName("Should find slot by task and start time")
        void shouldFindByTaskAndStartTime() {
            // Act
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findByTaskAndStartTime(
                testTask,
                LocalTime.of(9, 0)
            );

            // Assert
            assertThat(foundSlot).isPresent();
            assertThat(foundSlot.get().getEndTime()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("Should find slot by task ID and start time")
        void shouldFindByTaskIdAndStartTime() {
            // Act
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findByTaskIdAndStartTime(
                testTask.getId(),
                LocalTime.of(14, 0)
            );

            // Assert
            assertThat(foundSlot).isPresent();
            assertThat(foundSlot.get().getEndTime()).isEqualTo(LocalTime.of(15, 0));
        }

        @Test
        @DisplayName("Should return empty when start time not found")
        void shouldReturnEmptyWhenStartTimeNotFound() {
            // Act
            Optional<AppointmentSlot> foundSlot = appointmentSlotRepository.findByTaskAndStartTime(
                testTask,
                LocalTime.of(8, 0)
            );

            // Assert
            assertThat(foundSlot).isEmpty();
        }
    }

    @Nested
    @DisplayName("Existence Checks")
    class ExistenceTests {

        @Test
        @DisplayName("Should return true when slot exists with start time")
        void shouldReturnTrueWhenSlotExistsWithStartTime() {
            // Act
            boolean exists = appointmentSlotRepository.existsByTaskAndStartTime(
                testTask,
                LocalTime.of(9, 0)
            );

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when no slot with start time")
        void shouldReturnFalseWhenNoSlotWithStartTime() {
            // Act
            boolean exists = appointmentSlotRepository.existsByTaskAndStartTime(
                testTask,
                LocalTime.of(8, 0)
            );

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should check existence by time range")
        void shouldCheckExistenceByTimeRange() {
            // Act
            boolean exists = appointmentSlotRepository.existsByTaskAndStartTimeAndEndTime(
                testTask,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
            );

            // Assert
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Count and Sum Operations")
    class CountAndSumTests {

        @Test
        @DisplayName("Should count slots for task")
        void shouldCountByTask() {
            // Act
            long count = appointmentSlotRepository.countByTask(testTask);

            // Assert
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should sum capacity for task")
        void shouldSumCapacityByTaskId() {
            // Act
            int totalCapacity = appointmentSlotRepository.sumCapacityByTaskId(testTask.getId());

            // Assert - 5 + 10 + 3 = 18
            assertThat(totalCapacity).isEqualTo(18);
        }

        @Test
        @DisplayName("Should sum booked count for task")
        void shouldSumBookedCountByTaskId() {
            // Act
            int totalBooked = appointmentSlotRepository.sumBookedCountByTaskId(testTask.getId());

            // Assert - 2 + 0 + 3 = 5
            assertThat(totalBooked).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return zero for task with no slots")
        void shouldReturnZeroForTaskWithNoSlots() {
            // Act
            long count = appointmentSlotRepository.countByTask(anotherTask);
            int capacity = appointmentSlotRepository.sumCapacityByTaskId(anotherTask.getId());
            int booked = appointmentSlotRepository.sumBookedCountByTaskId(anotherTask.getId());

            // Assert
            assertThat(count).isZero();
            assertThat(capacity).isZero();
            assertThat(booked).isZero();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("Should delete all slots for task")
        void shouldDeleteByTask() {
            // Act
            appointmentSlotRepository.deleteByTask(testTask);
            entityManager.flush();

            // Assert
            List<AppointmentSlot> remainingSlots = appointmentSlotRepository.findByTask(testTask);
            assertThat(remainingSlots).isEmpty();
        }

        @Test
        @DisplayName("Should delete all slots for task ID")
        void shouldDeleteByTaskId() {
            // Act
            appointmentSlotRepository.deleteByTaskId(testTask.getId());
            entityManager.flush();

            // Assert
            List<AppointmentSlot> remainingSlots = appointmentSlotRepository.findByTaskId(testTask.getId());
            assertThat(remainingSlots).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find Fully Booked Slots")
    class FindFullyBookedSlotsTests {

        @Test
        @DisplayName("Should find fully booked slots")
        void shouldFindFullyBookedSlots() {
            // Act
            List<AppointmentSlot> fullSlots = appointmentSlotRepository.findFullyBookedSlotsByTask(testTask);

            // Assert
            assertThat(fullSlots).hasSize(1);
            assertThat(fullSlots.get(0).getStartTime()).isEqualTo(LocalTime.of(16, 0));
        }
    }

    @Nested
    @DisplayName("Timestamp Auto-Generation")
    class TimestampTests {

        @Test
        @DisplayName("Should set createdAt on persist")
        void shouldSetCreatedAtOnPersist() {
            // Arrange
            AppointmentSlot newSlot = new AppointmentSlot(
                testTask,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                5
            );

            // Act
            AppointmentSlot savedSlot = appointmentSlotRepository.save(newSlot);
            entityManager.flush();

            // Assert
            assertThat(savedSlot.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set updatedAt on persist")
        void shouldSetUpdatedAtOnPersist() {
            // Arrange
            AppointmentSlot newSlot = new AppointmentSlot(
                testTask,
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                5
            );

            // Act
            AppointmentSlot savedSlot = appointmentSlotRepository.save(newSlot);
            entityManager.flush();

            // Assert
            assertThat(savedSlot.getUpdatedAt()).isNotNull();
            assertThat(savedSlot.getUpdatedAt()).isEqualTo(savedSlot.getCreatedAt());
        }
    }
}
