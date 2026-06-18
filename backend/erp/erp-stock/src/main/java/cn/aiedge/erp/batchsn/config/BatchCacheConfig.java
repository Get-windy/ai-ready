package cn.aiedge.erp.batchsn.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * 批次管理模块缓存配置
 *
 * @author team-member
 * @date 2026-05-01
 */
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "batch.cache")
@Data
public class BatchCacheConfig {
    
    // 缓存配置属性
    private DetailConfig detail;
    private ListConfig list;
    private StockConfig stock;
    private ExpiringConfig expiring;
    private NullValueConfig nullValue;
    private LockConfig lock;
    private PrefixConfig prefix;
    
    @Data
    public static class DetailConfig {
        private long redisTtl;
        private long localTtl;
        private long maxSize;
    }
    
    @Data
    public static class ListConfig {
        private long redisTtl;
        private long maxSize;
    }
    
    @Data
    public static class StockConfig {
        private long redisTtl;
        private long maxSize;
    }
    
    @Data
    public static class ExpiringConfig {
        private long redisTtl;
        private long maxSize;
    }
    
    @Data
    public static class NullValueConfig {
        private long redisTtl;
    }
    
    @Data
    public static class LockConfig {
        private long ttl;
    }
    
    @Data
    public static class PrefixConfig {
        private String detail;
        private String list;
        private String stock;
        private String expiring;
        private String lock;
    }
    
    /**
     * 配置RedisTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 设置key序列化器
        template.setKeySerializer(new StringRedisSerializer());
        // 设置value序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // 设置hash key序列化器
        template.setHashKeySerializer(new StringRedisSerializer());
        // 设置hash value序列化器
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 配置RedisCacheManager
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisCacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(detail != null ? detail.getRedisTtl() : 1800))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        // 针对不同类型的缓存配置不同的TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        if (detail != null) {
            cacheConfigurations.put("batchDetail", defaultConfig.entryTtl(Duration.ofSeconds(detail.getRedisTtl())));
        }
        
        if (list != null) {
            cacheConfigurations.put("batchList", defaultConfig.entryTtl(Duration.ofSeconds(list.getRedisTtl())));
        }
        
        if (stock != null) {
            cacheConfigurations.put("stockSummary", defaultConfig.entryTtl(Duration.ofSeconds(stock.getRedisTtl())));
        }
        
        if (expiring != null) {
            cacheConfigurations.put("expiringWarning", defaultConfig.entryTtl(Duration.ofSeconds(expiring.getRedisTtl())));
        }
        
        if (nullValue != null) {
            cacheConfigurations.put("nullValue", defaultConfig.entryTtl(Duration.ofSeconds(nullValue.getRedisTtl())).disableCachingNullValues());
        }
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
    
    /**
     * 批次详情本地缓存（Caffeine）
     */
    @Bean
    public Cache<String, Object> batchDetailLocalCache() {
        long ttl = detail != null ? detail.getLocalTtl() : 300;
        long maxSize = detail != null ? detail.getMaxSize() : 10000;
        
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ttl))
            .maximumSize(maxSize)
            .recordStats()
            .build();
    }
    
    /**
     * 批次列表本地缓存
     */
    @Bean
    public Cache<String, Object> batchListLocalCache() {
        long maxSize = list != null ? list.getMaxSize() : 1000;
        
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(maxSize)
            .recordStats()
            .build();
    }
    
    /**
     * 库存汇总本地缓存
     */
    @Bean
    public Cache<String, Object> stockSummaryLocalCache() {
        long maxSize = stock != null ? stock.getMaxSize() : 100;
        
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(maxSize)
            .recordStats()
            .build();
    }
    
    /**
     * 临期预警本地缓存
     */
    @Bean
    public Cache<String, Object> expiringWarningLocalCache() {
        long maxSize = expiring != null ? expiring.getMaxSize() : 100;
        
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(maxSize)
            .recordStats()
            .build();
    }
    
    /**
     * 获取批次详情缓存前缀
     */
    public String getBatchDetailPrefix() {
        return prefix != null && prefix.getDetail() != null ? prefix.getDetail() : "batch:detail:";
    }
    
    /**
     * 获取批次列表缓存前缀
     */
    public String getBatchListPrefix() {
        return prefix != null && prefix.getList() != null ? prefix.getList() : "batch:list:";
    }
    
    /**
     * 获取库存汇总缓存前缀
     */
    public String getStockSummaryPrefix() {
        return prefix != null && prefix.getStock() != null ? prefix.getStock() : "batch:stock:";
    }
    
    /**
     * 获取临期预警缓存前缀
     */
    public String getExpiringWarningPrefix() {
        return prefix != null && prefix.getExpiring() != null ? prefix.getExpiring() : "batch:expiring:";
    }
    
    /**
     * 获取分布式锁缓存前缀
     */
    public String getLockPrefix() {
        return prefix != null && prefix.getLock() != null ? prefix.getLock() : "batch:lock:";
    }
}