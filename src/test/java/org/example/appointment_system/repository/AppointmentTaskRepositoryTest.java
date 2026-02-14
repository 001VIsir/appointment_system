package org.example.appointment_system.repository;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AppointmentTaskRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class AppointmentTaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppointmentTaskRepository appointmentTaskRepository;

    private MerchantProfile testMerchant;
    private ServiceItem testService;
    private ServiceItem anotherService;
    private AppointmentTask morningTask;
    private AppointmentTask afternoonTask;
    private AppointmentTask inactiveTask;

    @BeforeEach
    void setUp() {
        // Create merchant user and profile
        User merchantUser = new User("merchant1", "password", "merchant1@example.com", UserRole.MERCHANT);
        entityManager.persist(merchantUser);

        testMerchant = new MerchantProfile(merchantUser, "Test Business");
        entityManager.persist(testMerchant);

        // Create service items
        testService = new ServiceItem(
            testMerchant,
            "Haircut",
            "Professional haircut service",
            ServiceCategory.BEAUTY,
            30,
            new BigDecimal("25.00"),
            true
        );
        anotherService = new ServiceItem(
            testMerchant,
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
        morningTask = new AppointmentTask(
            testService,
            "Morning Appointments",
            "Available from 9am to 12pm",
            LocalDate.of(2026, 2, 20),
            10,
            true
        );
        afternoonTask = new AppointmentTask(
            testService,
            "Afternoon Appointments",
            "Available from 2pm to 6pm",
            LocalDate.of(2026, 2, 20),
            15,
            true
        );
        inactiveTask = new AppointmentTask(
            testService,
            "Cancelled Task",
            "This task is cancelled",
            LocalDate.of(2026, 2, 21),
            5,
            false
        );

        entityManager.persist(morningTask);
        entityManager.persist(afternoonTask);
        entityManager.persist(inactiveTask);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and find appointment task by ID")
        void shouldSaveAndFindById() {
            // Arrange
            AppointmentTask newTask = new AppointmentTask(
                testService,
                "New Task",
                LocalDate.of(2026, 3, 1),
                20
            );

            // Act
            AppointmentTask savedTask = appointmentTaskRepository.save(newTask);
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findById(savedTask.getId());

            // Assert
            assertThat(foundTask).isPresent();
            assertThat(foundTask.get().getTitle()).isEqualTo("New Task");
        }

        @Test
        @DisplayName("Should update appointment task")
        void shouldUpdateTask() {
            // Arrange
            morningTask.setTotalCapacity(25);

            // Act
            AppointmentTask updatedTask = appointmentTaskRepository.save(morningTask);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findById(updatedTask.getId());
            assertThat(foundTask).isPresent();
            assertThat(foundTask.get().getTotalCapacity()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should delete appointment task")
        void shouldDeleteTask() {
            // Act
            appointmentTaskRepository.delete(morningTask);
            entityManager.flush();

            // Assert
            Optional<AppointmentTask> deletedTask = appointmentTaskRepository.findById(morningTask.getId());
            assertThat(deletedTask).isEmpty();
        }

        @Test
        @DisplayName("Should find all appointment tasks")
        void shouldFindAll() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findAll();

            // Assert
            assertThat(tasks).hasSize(3);
        }

        @Test
        @DisplayName("Should count appointment tasks")
        void shouldCount() {
            // Act
            long count = appointmentTaskRepository.count();

            // Assert
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Find by Service")
    class FindByServiceTests {

        @Test
        @DisplayName("Should find all tasks by service")
        void shouldFindAllByService() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByService(testService);

            // Assert
            assertThat(tasks).hasSize(3);
            assertThat(tasks).extracting(AppointmentTask::getTitle)
                .containsExactlyInAnyOrder("Morning Appointments", "Afternoon Appointments", "Cancelled Task");
        }

        @Test
        @DisplayName("Should find all tasks by service ID")
        void shouldFindAllByServiceId() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByServiceId(testService.getId());

            // Assert
            assertThat(tasks).hasSize(3);
        }

        @Test
        @DisplayName("Should return empty list for service with no tasks")
        void shouldReturnEmptyForServiceWithNoTasks() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByService(anotherService);

            // Assert
            assertThat(tasks).isEmpty();
        }

        @Test
        @DisplayName("Should find only active tasks by service")
        void shouldFindActiveByService() {
            // Act
            List<AppointmentTask> activeTasks = appointmentTaskRepository.findByServiceAndActiveTrue(testService);

            // Assert
            assertThat(activeTasks).hasSize(2);
            assertThat(activeTasks).extracting(AppointmentTask::getTitle)
                .containsExactlyInAnyOrder("Morning Appointments", "Afternoon Appointments")
                .doesNotContain("Cancelled Task");
        }

        @Test
        @DisplayName("Should find only active tasks by service ID")
        void shouldFindActiveByServiceId() {
            // Act
            List<AppointmentTask> activeTasks = appointmentTaskRepository.findByServiceIdAndActiveTrue(testService.getId());

            // Assert
            assertThat(activeTasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find inactive tasks by service")
        void shouldFindInactiveByService() {
            // Act
            List<AppointmentTask> inactiveTasks = appointmentTaskRepository.findByServiceAndActiveFalse(testService);

            // Assert
            assertThat(inactiveTasks).hasSize(1);
            assertThat(inactiveTasks.get(0).getTitle()).isEqualTo("Cancelled Task");
        }
    }

    @Nested
    @DisplayName("Find by ID and Service")
    class FindByIdAndServiceTests {

        @Test
        @DisplayName("Should find task by ID and service")
        void shouldFindByIdAndService() {
            // Act
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findByIdAndService(
                morningTask.getId(),
                testService
            );

            // Assert
            assertThat(foundTask).isPresent();
            assertThat(foundTask.get().getTitle()).isEqualTo("Morning Appointments");
        }

        @Test
        @DisplayName("Should return empty when task belongs to different service")
        void shouldReturnEmptyWhenTaskBelongsToDifferentService() {
            // Act
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findByIdAndService(
                morningTask.getId(),
                anotherService
            );

            // Assert
            assertThat(foundTask).isEmpty();
        }

        @Test
        @DisplayName("Should find task by ID and service ID")
        void shouldFindByIdAndServiceId() {
            // Act
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findByIdAndServiceId(
                morningTask.getId(),
                testService.getId()
            );

            // Assert
            assertThat(foundTask).isPresent();
            assertThat(foundTask.get().getTitle()).isEqualTo("Morning Appointments");
        }
    }

    @Nested
    @DisplayName("Find by Date")
    class FindByDateTests {

        @Test
        @DisplayName("Should find tasks by date")
        void shouldFindByDate() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByTaskDate(LocalDate.of(2026, 2, 20));

            // Assert
            assertThat(tasks).hasSize(2);
            assertThat(tasks).extracting(AppointmentTask::getTitle)
                .containsExactlyInAnyOrder("Morning Appointments", "Afternoon Appointments");
        }

        @Test
        @DisplayName("Should find active tasks by date")
        void shouldFindActiveByDate() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByTaskDateAndActiveTrue(LocalDate.of(2026, 2, 20));

            // Assert
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find tasks by service and date")
        void shouldFindByServiceAndDate() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByServiceAndTaskDate(
                testService,
                LocalDate.of(2026, 2, 20)
            );

            // Assert
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find tasks by service ID and date")
        void shouldFindByServiceIdAndDate() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByServiceIdAndTaskDate(
                testService.getId(),
                LocalDate.of(2026, 2, 20)
            );

            // Assert
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find tasks between dates")
        void shouldFindBetweenDates() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByTaskDateBetween(
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22)
            );

            // Assert
            assertThat(tasks).hasSize(3);
        }

        @Test
        @DisplayName("Should find active tasks between dates")
        void shouldFindActiveBetweenDates() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByTaskDateBetweenAndActiveTrue(
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22)
            );

            // Assert
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find active tasks for service between dates")
        void shouldFindActiveForServiceBetweenDates() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByServiceAndTaskDateBetweenAndActiveTrue(
                testService,
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22)
            );

            // Assert
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find tasks with date >= specified date")
        void shouldFindByDateGreaterThanOrEqual() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByServiceAndTaskDateGreaterThanEqualAndActiveTrue(
                testService,
                LocalDate.of(2026, 2, 20)
            );

            // Assert
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("Should find tasks by service ID with date >= specified date")
        void shouldFindByServiceIdAndDateGreaterThanOrEqual() {
            // Act
            List<AppointmentTask> tasks = appointmentTaskRepository.findByServiceIdAndTaskDateGreaterThanEqualAndActiveTrue(
                testService.getId(),
                LocalDate.of(2026, 2, 20)
            );

            // Assert
            assertThat(tasks).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Existence Checks")
    class ExistenceTests {

        @Test
        @DisplayName("Should return true when task exists for service on date")
        void shouldReturnTrueWhenTaskExistsForServiceOnDate() {
            // Act
            boolean exists = appointmentTaskRepository.existsByServiceAndTaskDate(
                testService,
                LocalDate.of(2026, 2, 20)
            );

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when no task for service on date")
        void shouldReturnFalseWhenNoTaskForServiceOnDate() {
            // Act
            boolean exists = appointmentTaskRepository.existsByServiceAndTaskDate(
                testService,
                LocalDate.of(2026, 3, 1)
            );

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should check existence by service ID and date")
        void shouldCheckExistenceByServiceIdAndDate() {
            // Act
            boolean exists = appointmentTaskRepository.existsByServiceIdAndTaskDate(
                testService.getId(),
                LocalDate.of(2026, 2, 20)
            );

            // Assert
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountTests {

        @Test
        @DisplayName("Should count all tasks for service")
        void shouldCountAllByService() {
            // Act
            long count = appointmentTaskRepository.countByService(testService);

            // Assert
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count active tasks for service")
        void shouldCountActiveByService() {
            // Act
            long count = appointmentTaskRepository.countByServiceAndActiveTrue(testService);

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero count for service with no tasks")
        void shouldReturnZeroForServiceWithNoTasks() {
            // Act
            long count = appointmentTaskRepository.countByService(anotherService);

            // Assert
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Find by ID and Active")
    class FindByIdAndActiveTests {

        @Test
        @DisplayName("Should find task by ID and active status")
        void shouldFindByIdAndActive() {
            // Act
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findByIdAndActive(
                morningTask.getId(),
                true
            );

            // Assert
            assertThat(foundTask).isPresent();
            assertThat(foundTask.get().getTitle()).isEqualTo("Morning Appointments");
        }

        @Test
        @DisplayName("Should return empty when task is inactive")
        void shouldReturnEmptyWhenInactive() {
            // Act
            Optional<AppointmentTask> foundTask = appointmentTaskRepository.findByIdAndActive(
                inactiveTask.getId(),
                true
            );

            // Assert
            assertThat(foundTask).isEmpty();
        }
    }

    @Nested
    @DisplayName("Timestamp Auto-Generation")
    class TimestampTests {

        @Test
        @DisplayName("Should set createdAt on persist")
        void shouldSetCreatedAtOnPersist() {
            // Arrange
            AppointmentTask newTask = new AppointmentTask(
                testService,
                "New Timestamp Task",
                LocalDate.of(2026, 4, 1),
                10
            );

            // Act
            AppointmentTask savedTask = appointmentTaskRepository.save(newTask);
            entityManager.flush();

            // Assert
            assertThat(savedTask.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set updatedAt on persist")
        void shouldSetUpdatedAtOnPersist() {
            // Arrange
            AppointmentTask newTask = new AppointmentTask(
                testService,
                "New Timestamp Task 2",
                LocalDate.of(2026, 4, 2),
                10
            );

            // Act
            AppointmentTask savedTask = appointmentTaskRepository.save(newTask);
            entityManager.flush();

            // Assert
            assertThat(savedTask.getUpdatedAt()).isNotNull();
            assertThat(savedTask.getUpdatedAt()).isEqualTo(savedTask.getCreatedAt());
        }
    }
}
