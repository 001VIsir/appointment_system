package org.example.appointment_system.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.enums.BookingStatus;
import org.example.appointment_system.repository.BookingRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.example.appointment_system.repository.ServiceItemRepository;
import org.example.appointment_system.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer Prometheus Metrics Configuration.
 *
 * <p>Configures custom metrics for the appointment system including:</p>
 * <ul>
 *   <li>Booking metrics (total, by status, success rate)</li>
 *   <li>User metrics (total, by role)</li>
 *   <li>Merchant metrics (total, active)</li>
 *   <li>Service item metrics (total, active)</li>
 * </ul>
 *
 * <h3>Available Metrics:</h3>
 * <ul>
 *   <li>{@code appointment.bookings.total} - Total number of bookings (gauge)</li>
 *   <li>{@code appointment.bookings.active} - Active bookings count (gauge)</li>
 *   <li>{@code appointment.bookings.status} - Bookings by status (gauge with status tag)</li>
 *   <li>{@code appointment.bookings.created} - Counter for created bookings</li>
 *   <li>{@code appointment.bookings.cancelled} - Counter for cancelled bookings</li>
 *   <li>{@code appointment.bookings.completed} - Counter for completed bookings</li>
 *   <li>{@code appointment.users.total} - Total users count (gauge)</li>
 *   <li>{@code appointment.merchants.total} - Total merchants count (gauge)</li>
 *   <li>{@code appointment.services.total} - Total service items (gauge)</li>
 *   <li>{@code appointment.services.active} - Active service items (gauge)</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MetricsConfig {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final MeterRegistry meterRegistry;

    // Atomic counters for tracking booking operations
    private final AtomicInteger bookingsCreatedCounter = new AtomicInteger(0);
    private final AtomicInteger bookingsCancelledCounter = new AtomicInteger(0);
    private final AtomicInteger bookingsCompletedCounter = new AtomicInteger(0);
    private final AtomicInteger bookingsConfirmedCounter = new AtomicInteger(0);

    /**
     * Configure the MeterRegistry with application-specific settings.
     * <ul>
     *   <li>Adds common tags (application name)</li>
     *   <li>Configures meter filters for percentile histograms</li>
     *   <li>Sets up distribution statistics for HTTP requests</li>
     * </ul>
     */
    @PostConstruct
    public void configureMeterRegistry() {
        // Add common tags to all metrics
        meterRegistry.config().commonTags(
                "application", "appointment_system",
                "version", "2.0.0"
        );

        // Configure meter filter for HTTP server requests
        meterRegistry.config().meterFilter(MeterFilter.acceptNameStartsWith("http.server.requests"));

        // Configure histogram buckets for response time percentiles
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .percentiles(0.5, 0.9, 0.95, 0.99)
                            .minimumExpectedValue(Duration.ofMillis(1).toNanos())
                            .maximumExpectedValue(Duration.ofSeconds(30).toNanos())
                            .build()
                            .merge(config);
                }
                return config;
            }
        });

        // Enable histogram for custom timers
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("appointment.")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .percentiles(0.5, 0.9, 0.95)
                            .build()
                            .merge(config);
                }
                return config;
            }
        });

        log.info("Micrometer metrics configured with common tags and filters");
    }

    /**
     * Register booking metrics as gauges.
     * These gauges reflect the current state from the database.
     */
    @Bean
    public Gauge bookingsTotalGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.bookings.total", bookingRepository, BookingRepository::count)
                .description("Total number of bookings")
                .tag("type", "all")
                .register(registry);
    }

    @Bean
    public Gauge bookingsPendingGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.bookings.status", () ->
                        bookingRepository.countByStatus(BookingStatus.PENDING))
                .description("Number of bookings by status")
                .tag("status", "pending")
                .register(registry);
    }

    @Bean
    public Gauge bookingsConfirmedGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.bookings.status", () ->
                        bookingRepository.countByStatus(BookingStatus.CONFIRMED))
                .description("Number of bookings by status")
                .tag("status", "confirmed")
                .register(registry);
    }

    @Bean
    public Gauge bookingsCancelledGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.bookings.status", () ->
                        bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .description("Number of bookings by status")
                .tag("status", "cancelled")
                .register(registry);
    }

    @Bean
    public Gauge bookingsCompletedGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.bookings.status", () ->
                        bookingRepository.countByStatus(BookingStatus.COMPLETED))
                .description("Number of bookings by status")
                .tag("status", "completed")
                .register(registry);
    }

    @Bean
    public Gauge bookingsActiveGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.bookings.active", () -> {
                    long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
                    long confirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
                    return pending + confirmed;
                })
                .description("Number of active bookings (pending + confirmed)")
                .register(registry);
    }

    /**
     * Register user metrics as gauges.
     */
    @Bean
    public Gauge usersTotalGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.users.total", userRepository, UserRepository::count)
                .description("Total number of users")
                .tag("type", "all")
                .register(registry);
    }

    @Bean
    public Gauge usersEnabledGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.users.enabled", () ->
                        userRepository.findByEnabledTrue().size())
                .description("Number of enabled users")
                .tag("status", "enabled")
                .register(registry);
    }

    @Bean
    public Gauge usersDisabledGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.users.enabled", () ->
                        userRepository.findByEnabledFalse().size())
                .description("Number of disabled users")
                .tag("status", "disabled")
                .register(registry);
    }

    /**
     * Register merchant metrics as gauges.
     */
    @Bean
    public Gauge merchantsTotalGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.merchants.total", merchantProfileRepository, MerchantProfileRepository::count)
                .description("Total number of merchants")
                .register(registry);
    }

    /**
     * Register service item metrics as gauges.
     */
    @Bean
    public Gauge servicesTotalGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.services.total", serviceItemRepository, ServiceItemRepository::count)
                .description("Total number of service items")
                .register(registry);
    }

    @Bean
    public Gauge servicesActiveGauge(MeterRegistry registry) {
        return Gauge.builder("appointment.services.active", () ->
                        serviceItemRepository.findAll().stream()
                                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                                .count())
                .description("Number of active service items")
                .register(registry);
    }

    /**
     * Counter for booking creation events.
     * Increment this counter when a new booking is created.
     */
    @Bean
    public Counter bookingCreatedCounter(MeterRegistry registry) {
        return Counter.builder("appointment.bookings.created")
                .description("Count of bookings created")
                .tag("operation", "create")
                .register(registry);
    }

    /**
     * Counter for booking cancellation events.
     */
    @Bean
    public Counter bookingCancelledCounter(MeterRegistry registry) {
        return Counter.builder("appointment.bookings.cancelled")
                .description("Count of bookings cancelled")
                .tag("operation", "cancel")
                .register(registry);
    }

    /**
     * Counter for booking completion events.
     */
    @Bean
    public Counter bookingCompletedCounter(MeterRegistry registry) {
        return Counter.builder("appointment.bookings.completed")
                .description("Count of bookings completed")
                .tag("operation", "complete")
                .register(registry);
    }

    /**
     * Counter for booking confirmation events.
     */
    @Bean
    public Counter bookingConfirmedCounter(MeterRegistry registry) {
        return Counter.builder("appointment.bookings.confirmed")
                .description("Count of bookings confirmed")
                .tag("operation", "confirm")
                .register(registry);
    }

    /**
     * Helper method to create tags for booking metrics.
     */
    public static Tags bookingTags(Long merchantId, Long serviceId, Long taskId) {
        return Tags.of(
                Tag.of("merchant_id", merchantId != null ? merchantId.toString() : "unknown"),
                Tag.of("service_id", serviceId != null ? serviceId.toString() : "unknown"),
                Tag.of("task_id", taskId != null ? taskId.toString() : "unknown")
        );
    }

    /**
     * Helper method to create tags for error metrics.
     */
    public static Tags errorTags(String errorType, int statusCode) {
        return Tags.of(
                Tag.of("error_type", errorType),
                Tag.of("status_code", String.valueOf(statusCode))
        );
    }
}
