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
 * Spring缓存配置，使用Redis作为后端。
 *
 * <p>此配置使用Spring缓存注解启用声明式缓存，
 * 使用Redis作为缓存存储。提供：</p>
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>@EnableCaching用于声明式缓存支持</li>
 *   <li>Redis支持的缓存存储</li>
 *   <li>每个缓存可配置的TTL</li>
 *   <li>缓存值的JSON序列化</li>
 *   <li>禁用空值缓存</li>
 * </ul>
 *
 * <h3>缓存区域：</h3>
 * <ul>
 *   <li>merchantProfiles: 30分钟TTL</li>
 *   <li>merchantSettings: 30分钟TTL</li>
 *   <li>serviceItems: 15分钟TTL</li>
 *   <li>serviceItemLists: 10分钟TTL</li>
 *   <li>appointmentTasks: 5分钟TTL</li>
 *   <li>publicTasks: 2分钟TTL</li>
 * </ul>
 *
 * <h3>用法：</h3>
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

    // 缓存名称及其默认TTL（分钟）
    public static final String CACHE_MERCHANT_PROFILES = "merchantProfiles";
    public static final String CACHE_MERCHANT_SETTINGS = "merchantSettings";
    public static final String CACHE_SERVICE_ITEMS = "serviceItems";
    public static final String CACHE_SERVICE_ITEM_LISTS = "serviceItemLists";
    public static final String CACHE_APPOINTMENT_TASKS = "appointmentTasks";
    public static final String CACHE_PUBLIC_TASKS = "publicTasks";
    public static final String CACHE_SLOTS = "slots";

    // TTL配置（分钟）
    private static final int TTL_MERCHANT_PROFILES = 30;
    private static final int TTL_MERCHANT_SETTINGS = 30;
    private static final int TTL_SERVICE_ITEMS = 15;
    private static final int TTL_SERVICE_ITEM_LISTS = 10;
    private static final int TTL_APPOINTMENT_TASKS = 5;
    private static final int TTL_PUBLIC_TASKS = 2;
    private static final int TTL_SLOTS = 1;
    private static final int TTL_DEFAULT = 10;

    /**
     * 创建Redis缓存管理器。
     *
     * <p>配置特定缓存的TTL和序列化设置。</p>
     *
     * @param connectionFactory Redis连接工厂
     * @return 配置好的CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 创建默认缓存配置
        RedisCacheConfiguration defaultConfig = createCacheConfiguration(Duration.ofMinutes(TTL_DEFAULT));

        // 创建特定缓存的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 商家相关缓存（较长TTL）
        cacheConfigurations.put(CACHE_MERCHANT_PROFILES,
            createCacheConfiguration(Duration.ofMinutes(TTL_MERCHANT_PROFILES)));
        cacheConfigurations.put(CACHE_MERCHANT_SETTINGS,
            createCacheConfiguration(Duration.ofMinutes(TTL_MERCHANT_SETTINGS)));

        // 服务项目缓存
        cacheConfigurations.put(CACHE_SERVICE_ITEMS,
            createCacheConfiguration(Duration.ofMinutes(TTL_SERVICE_ITEMS)));
        cacheConfigurations.put(CACHE_SERVICE_ITEM_LISTS,
            createCacheConfiguration(Duration.ofMinutes(TTL_SERVICE_ITEM_LISTS)));

        // 预约任务缓存（因频繁更新使用较短TTL）
        cacheConfigurations.put(CACHE_APPOINTMENT_TASKS,
            createCacheConfiguration(Duration.ofMinutes(TTL_APPOINTMENT_TASKS)));
        cacheConfigurations.put(CACHE_PUBLIC_TASKS,
            createCacheConfiguration(Duration.ofMinutes(TTL_PUBLIC_TASKS)));

        // 时段缓存（因预约更新使用非常短的TTL）
        cacheConfigurations.put(CACHE_SLOTS,
            createCacheConfiguration(Duration.ofMinutes(TTL_SLOTS)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }

    /**
     * 创建具有指定TTL的缓存配置。
     *
     * @param ttl 生存时间
     * @return 缓存配置
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
     * 创建用于缓存序列化的配置好的ObjectMapper。
     *
     * @return 配置好的ObjectMapper
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
