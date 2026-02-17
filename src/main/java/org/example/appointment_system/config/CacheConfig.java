package org.example.appointment_system.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache Configuration with Redis backend.
 *
 * <p>This configuration enables declarative caching using Spring Cache annotations
 * with Redis as the cache store. It provides:</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>@EnableCaching for declarative cache support</li>
 *   <li>Redis-backed cache storage</li>
 *   <li>Configurable TTL per cache</li>
 *   <li>JSON serialization for cached values</li>
 *   <li>Null value caching disabled</li>
 * </ul>
 *
 * <h3>Cache Regions:</h3>
 * <ul>
 *   <li>merchantProfiles: 30 minutes TTL</li>
 *   <li>merchantSettings: 30 minutes TTL</li>
 *   <li>serviceItems: 15 minutes TTL</li>
 *   <li>serviceItemLists: 10 minutes TTL</li>
 *   <li>appointmentTasks: 5 minutes TTL</li>
 *   <li>publicTasks: 2 minutes TTL</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * &#64;Cacheable(value = "merchantProfiles", key = "#userId")
 * public MerchantProfile getProfileByUserId(Long userId) { ... }
 *
 * &#64;CacheEvict(value = "merchantProfiles", key = "#userId")
 * public void updateProfile(Long userId, MerchantProfile profile) { ... }
 *
 * &#64;CacheEvict(value = "merchantProfiles", allEntries = true)
 * public void clearAllProfiles() { ... }
 * </pre>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names with their default TTL in minutes
    public static final String CACHE_MERCHANT_PROFILES = "merchantProfiles";
    public static final String CACHE_MERCHANT_SETTINGS = "merchantSettings";
    public static final String CACHE_SERVICE_ITEMS = "serviceItems";
    public static final String CACHE_SERVICE_ITEM_LISTS = "serviceItemLists";
    public static final String CACHE_APPOINTMENT_TASKS = "appointmentTasks";
    public static final String CACHE_PUBLIC_TASKS = "publicTasks";
    public static final String CACHE_SLOTS = "slots";

    // TTL configurations (in minutes)
    private static final int TTL_MERCHANT_PROFILES = 30;
    private static final int TTL_MERCHANT_SETTINGS = 30;
    private static final int TTL_SERVICE_ITEMS = 15;
    private static final int TTL_SERVICE_ITEM_LISTS = 10;
    private static final int TTL_APPOINTMENT_TASKS = 5;
    private static final int TTL_PUBLIC_TASKS = 2;
    private static final int TTL_SLOTS = 1;
    private static final int TTL_DEFAULT = 10;

    /**
     * Create the Redis Cache Manager.
     *
     * <p>Configures cache-specific TTLs and serialization settings.</p>
     *
     * @param connectionFactory the Redis connection factory
     * @return the configured CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create default cache configuration
        RedisCacheConfiguration defaultConfig = createCacheConfiguration(Duration.ofMinutes(TTL_DEFAULT));

        // Create cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Merchant-related caches (longer TTL)
        cacheConfigurations.put(CACHE_MERCHANT_PROFILES,
            createCacheConfiguration(Duration.ofMinutes(TTL_MERCHANT_PROFILES)));
        cacheConfigurations.put(CACHE_MERCHANT_SETTINGS,
            createCacheConfiguration(Duration.ofMinutes(TTL_MERCHANT_SETTINGS)));

        // Service item caches
        cacheConfigurations.put(CACHE_SERVICE_ITEMS,
            createCacheConfiguration(Duration.ofMinutes(TTL_SERVICE_ITEMS)));
        cacheConfigurations.put(CACHE_SERVICE_ITEM_LISTS,
            createCacheConfiguration(Duration.ofMinutes(TTL_SERVICE_ITEM_LISTS)));

        // Appointment task caches (shorter TTL due to frequent updates)
        cacheConfigurations.put(CACHE_APPOINTMENT_TASKS,
            createCacheConfiguration(Duration.ofMinutes(TTL_APPOINTMENT_TASKS)));
        cacheConfigurations.put(CACHE_PUBLIC_TASKS,
            createCacheConfiguration(Duration.ofMinutes(TTL_PUBLIC_TASKS)));

        // Slot cache (very short TTL due to booking updates)
        cacheConfigurations.put(CACHE_SLOTS,
            createCacheConfiguration(Duration.ofMinutes(TTL_SLOTS)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }

    /**
     * Create a cache configuration with the specified TTL.
     *
     * @param ttl the time-to-live duration
     * @return the cache configuration
     */
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        ObjectMapper objectMapper = createObjectMapper();

        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
            .entryTtl(ttl)
            .disableCachingNullValues()
            .prefixCacheNameWith("appointment:cache:");
    }

    /**
     * Create a configured ObjectMapper for cache serialization.
     *
     * @return configured ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }
}
