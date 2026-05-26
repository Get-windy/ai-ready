package cn.aiedge.config;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Redis缓存优化配置
 * 
 * 优化策略：
 * 1. 分级缓存时间（热点数据短缓存、基础数据长缓存）
 * 2. JSON序列化（提高可读性和兼容性）
 * 3. 缓存空值处理（防止缓存穿透）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheOptimizationConfig {

    /**
     * 缓存配置常量
     */
    public static final String CACHE_USER = "user";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_MENU = "menu";
    public static final String CACHE_PERMISSION = "permission";
    public static final String CACHE_CUSTOMER = "customer";
    public static final String CACHE_ORDER = "order";
    public static final String CACHE_PRODUCT = "product";
    public static final String CACHE_CONFIG = "config";
    
    // 缓存时间配置（秒）
    private static final Duration TTL_HOT = Duration.ofMinutes(5);      // 热点数据5分钟
    private static final Duration TTL_NORMAL = Duration.ofMinutes(30);   // 普通数据30分钟
    private static final Duration TTL_STATIC = Duration.ofHours(2);      // 静态数据2小时
    private static final Duration TTL_CONFIG = Duration.ofHours(24);     // 配置数据24小时

    /**
     * Redis缓存管理器
     * 
     * 针对不同类型数据设置不同的缓存策略
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(TTL_NORMAL)
                .disableCachingNullValues(); // 默认不缓存空值
        
        // 针对不同缓存区域设置不同TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户数据：热点数据，短缓存
        cacheConfigurations.put(CACHE_USER, defaultConfig.entryTtl(TTL_HOT));
        
        // 角色权限：频繁访问，中等缓存
        cacheConfigurations.put(CACHE_ROLE, defaultConfig.entryTtl(TTL_NORMAL));
        cacheConfigurations.put(CACHE_PERMISSION, defaultConfig.entryTtl(TTL_NORMAL));
        
        // 菜单数据：相对静态，长缓存
        cacheConfigurations.put(CACHE_MENU, defaultConfig.entryTtl(TTL_STATIC));
        
        // 业务数据：热点数据，短缓存
        cacheConfigurations.put(CACHE_CUSTOMER, defaultConfig.entryTtl(TTL_HOT));
        cacheConfigurations.put(CACHE_ORDER, defaultConfig.entryTtl(TTL_HOT));
        cacheConfigurations.put(CACHE_PRODUCT, defaultConfig.entryTtl(TTL_NORMAL));
        
        // 配置数据：几乎不变，最长缓存
        cacheConfigurations.put(CACHE_CONFIG, defaultConfig.entryTtl(TTL_CONFIG));
        
        log.info("[Redis-Cache] Cache manager initialized with {} cache regions", cacheConfigurations.size());
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
