package cn.aiedge.cache.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 默认缓存过期时间（秒）
     */
    private static final long DEFAULT_TTL = 3600;

    /**
     * 用户缓存过期时间
     */
    private static final long USER_CACHE_TTL = 1800;

    /**
     * 权限缓存过期时间
     */
    private static final long PERMISSION_CACHE_TTL = 7200;

    /**
     * 数据缓存过期时间
     */
    private static final long DATA_CACHE_TTL = 600;

    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // Key序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value序列化 - 使用JSON序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(DEFAULT_TTL))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("ai-ready:");

        // 不同缓存的个性化配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户信息缓存 - 30分钟
        cacheConfigurations.put(CacheNames.USER_INFO, defaultConfig.entryTtl(Duration.ofSeconds(USER_CACHE_TTL)));
        cacheConfigurations.put(CacheNames.USER_TOKEN, defaultConfig.entryTtl(Duration.ofSeconds(USER_CACHE_TTL)));
        
        // 权限缓存 - 2小时
        cacheConfigurations.put(CacheNames.USER_PERMISSIONS, defaultConfig.entryTtl(Duration.ofSeconds(PERMISSION_CACHE_TTL)));
        cacheConfigurations.put(CacheNames.USER_ROLES, defaultConfig.entryTtl(Duration.ofSeconds(PERMISSION_CACHE_TTL)));
        cacheConfigurations.put(CacheNames.ROLE_PERMISSIONS, defaultConfig.entryTtl(Duration.ofSeconds(PERMISSION_CACHE_TTL)));
        
        // 业务数据缓存 - 10分钟
        cacheConfigurations.put(CacheNames.PRODUCT_DATA, defaultConfig.entryTtl(Duration.ofSeconds(DATA_CACHE_TTL)));
        cacheConfigurations.put(CacheNames.CUSTOMER_DATA, defaultConfig.entryTtl(Duration.ofSeconds(DATA_CACHE_TTL)));
        cacheConfigurations.put(CacheNames.ORDER_DATA, defaultConfig.entryTtl(Duration.ofSeconds(DATA_CACHE_TTL)));
        cacheConfigurations.put(CacheNames.STOCK_DATA, defaultConfig.entryTtl(Duration.ofSeconds(DATA_CACHE_TTL)));
        
        // 系统配置缓存 - 1小时
        cacheConfigurations.put(CacheNames.SYS_CONFIG, defaultConfig.entryTtl(Duration.ofSeconds(DEFAULT_TTL)));
        cacheConfigurations.put(CacheNames.DICT_DATA, defaultConfig.entryTtl(Duration.ofSeconds(DEFAULT_TTL)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
