package org.example.appointment_system.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.appointment_system.entity.Booking;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.entity.User;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.enums.UserRole;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MetricsConfig}.
 */
@ExtendWith(MockitoExtension.class)
class MetricsConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private ServiceItemRepository serviceItemRepository;

    private MeterRegistry meterRegistry;
    private MetricsConfig metricsConfig;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsConfig = new MetricsConfig(userRepository, bookingRepository,
                merchantProfileRepository, serviceItemRepository, meterRegistry);
    }

    @Nested
    @DisplayName("Booking Metrics Tests")
    class BookingMetricsTests {

        @Test
        @DisplayName("Should register bookings total gauge")
        void bookingsTotalGauge_shouldRegister() {
            when(bookingRepository.count()).thenReturn(100L);

            Gauge gauge = metricsConfig.bookingsTotalGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(100.0, gauge.value());
            verify(bookingRepository).count();
        }

        @Test
        @DisplayName("Should register bookings pending gauge")
        void bookingsPendingGauge_shouldRegister() {
            when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(10L);

            Gauge gauge = metricsConfig.bookingsPendingGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(10.0, gauge.value());
        }

        @Test
        @DisplayName("Should register bookings confirmed gauge")
        void bookingsConfirmedGauge_shouldRegister() {
            when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(50L);

            Gauge gauge = metricsConfig.bookingsConfirmedGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(50.0, gauge.value());
        }

        @Test
        @DisplayName("Should register bookings cancelled gauge")
        void bookingsCancelledGauge_shouldRegister() {
            when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(20L);

            Gauge gauge = metricsConfig.bookingsCancelledGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(20.0, gauge.value());
        }

        @Test
        @DisplayName("Should register bookings completed gauge")
        void bookingsCompletedGauge_shouldRegister() {
            when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(20L);

            Gauge gauge = metricsConfig.bookingsCompletedGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(20.0, gauge.value());
        }

        @Test
        @DisplayName("Should calculate active bookings as pending plus confirmed")
        void bookingsActiveGauge_shouldCalculateCorrectly() {
            when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(15L);
            when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(35L);

            Gauge gauge = metricsConfig.bookingsActiveGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(50.0, gauge.value());
        }
    }

    @Nested
    @DisplayName("User Metrics Tests")
    class UserMetricsTests {

        @Test
        @DisplayName("Should register users total gauge")
        void usersTotalGauge_shouldRegister() {
            when(userRepository.count()).thenReturn(500L);

            Gauge gauge = metricsConfig.usersTotalGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(500.0, gauge.value());
        }

        @Test
        @DisplayName("Should register users enabled gauge")
        void usersEnabledGauge_shouldRegister() {
            User enabledUser = mock(User.class);
            when(userRepository.findByEnabledTrue()).thenReturn(List.of(enabledUser, enabledUser, enabledUser));

            Gauge gauge = metricsConfig.usersEnabledGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(3.0, gauge.value());
        }

        @Test
        @DisplayName("Should register users disabled gauge")
        void usersDisabledGauge_shouldRegister() {
            when(userRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());

            Gauge gauge = metricsConfig.usersDisabledGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(0.0, gauge.value());
        }
    }

    @Nested
    @DisplayName("Merchant Metrics Tests")
    class MerchantMetricsTests {

        @Test
        @DisplayName("Should register merchants total gauge")
        void merchantsTotalGauge_shouldRegister() {
            when(merchantProfileRepository.count()).thenReturn(25L);

            Gauge gauge = metricsConfig.merchantsTotalGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(25.0, gauge.value());
        }
    }

    @Nested
    @DisplayName("Service Item Metrics Tests")
    class ServiceItemMetricsTests {

        @Test
        @DisplayName("Should register services total gauge")
        void servicesTotalGauge_shouldRegister() {
            when(serviceItemRepository.count()).thenReturn(100L);

            Gauge gauge = metricsConfig.servicesTotalGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(100.0, gauge.value());
        }

        @Test
        @DisplayName("Should register services active gauge")
        void servicesActiveGauge_shouldRegister() {
            ServiceItem activeService = mock(ServiceItem.class);
            when(activeService.getActive()).thenReturn(true);

            ServiceItem inactiveService = mock(ServiceItem.class);
            when(inactiveService.getActive()).thenReturn(false);

            when(serviceItemRepository.findAll()).thenReturn(Arrays.asList(activeService, inactiveService, activeService));

            Gauge gauge = metricsConfig.servicesActiveGauge(meterRegistry);

            assertNotNull(gauge);
            assertEquals(2.0, gauge.value());
        }
    }

    @Nested
    @DisplayName("Counter Tests")
    class CounterTests {

        @Test
        @DisplayName("Should register booking created counter")
        void bookingCreatedCounter_shouldRegister() {
            Counter counter = metricsConfig.bookingCreatedCounter(meterRegistry);

            assertNotNull(counter);
            counter.increment();
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should register booking cancelled counter")
        void bookingCancelledCounter_shouldRegister() {
            Counter counter = metricsConfig.bookingCancelledCounter(meterRegistry);

            assertNotNull(counter);
            counter.increment();
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should register booking completed counter")
        void bookingCompletedCounter_shouldRegister() {
            Counter counter = metricsConfig.bookingCompletedCounter(meterRegistry);

            assertNotNull(counter);
            counter.increment();
            assertEquals(1.0, counter.count());
        }

        @Test
        @DisplayName("Should register booking confirmed counter")
        void bookingConfirmedCounter_shouldRegister() {
            Counter counter = metricsConfig.bookingConfirmedCounter(meterRegistry);

            assertNotNull(counter);
            counter.increment();
            assertEquals(1.0, counter.count());
        }
    }

    @Nested
    @DisplayName("MeterRegistry Configuration Tests")
    class CustomizerTests {

        @Test
        @DisplayName("Should add common tags to registry")
        void configureMeterRegistry_shouldAddCommonTags() {
            // Call configureMeterRegistry (normally called by @PostConstruct)
            metricsConfig.configureMeterRegistry();

            // Create a test meter to verify tags
            Counter testCounter = Counter.builder("test.counter")
                    .register(meterRegistry);

            Meter.Id id = testCounter.getId();
            assertNotNull(id.getTag("application"));
            assertEquals("appointment_system", id.getTag("application"));
            assertNotNull(id.getTag("version"));
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should create booking tags with all values")
        void bookingTags_shouldCreateCorrectTags() {
            var tags = MetricsConfig.bookingTags(1L, 2L, 3L);

            assertNotNull(tags);
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("merchant_id") && t.getValue().equals("1")));
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("service_id") && t.getValue().equals("2")));
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("task_id") && t.getValue().equals("3")));
        }

        @Test
        @DisplayName("Should handle null values in booking tags")
        void bookingTags_shouldHandleNullValues() {
            var tags = MetricsConfig.bookingTags(null, null, null);

            assertNotNull(tags);
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("merchant_id") && t.getValue().equals("unknown")));
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("service_id") && t.getValue().equals("unknown")));
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("task_id") && t.getValue().equals("unknown")));
        }

        @Test
        @DisplayName("Should create error tags")
        void errorTags_shouldCreateCorrectTags() {
            var tags = MetricsConfig.errorTags("validation_error", 400);

            assertNotNull(tags);
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("error_type") && t.getValue().equals("validation_error")));
            assertTrue(tags.stream().anyMatch(t -> t.getKey().equals("status_code") && t.getValue().equals("400")));
        }
    }
}
