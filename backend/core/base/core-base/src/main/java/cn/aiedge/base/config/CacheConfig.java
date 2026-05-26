package cn.aiedge.base.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 
 * 配置多级缓存策略：
 * 1. Caffeine本地缓存 - 高频读取数据
 * 2. Redis分布式缓存 - 跨实例共享数据
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine本地缓存管理器
     * 适用于：用户基础信息、角色权限等高频读取数据
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
        );
        return cacheManager;
    }

    /**
     * Redis缓存管理器
     * 适用于：需要跨实例共享的缓存数据
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 不同缓存的过期时间配置
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // 用户缓存 - 30分钟
        configMap.put("user", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 角色缓存 - 1小时
        configMap.put("role", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 权限缓存 - 2小时
        configMap.put("permission", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // 登录会话缓存 - 24小时
        configMap.put("session", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 验证码缓存 - 5分钟
        configMap.put("captcha", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()
                .build();
    }
}
