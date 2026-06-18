package cn.aiedge.erp.sales.pricing.config;

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
 * 缓存配置类
 * 配置Redis作为二级缓存，支持价格策略缓存和价格计算结果缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 缓存名称常量
     */
    public static class CacheNames {
        // 价格策略缓存 - TTL: 10分钟
        public static final String PRICE_STRATEGIES = "priceStrategies";
        // 价格计算结果缓存 - TTL: 5分钟
        public static final String PRICE_CALCULATIONS = "priceCalculations";
        // Drools规则缓存 - TTL: 30分钟
        public static final String DROOLS_RULES = "droolsRules";
        // 客户等级缓存 - TTL: 1小时
        public static final String CUSTOMER_LEVELS = "customerLevels";
        // 产品类别缓存 - TTL: 1小时
        public static final String PRODUCT_CATEGORIES = "productCategories";
        // 租户配置缓存 - TTL: 2小时
        public static final String TENANT_CONFIGS = "tenantConfigs";
    }
    
    /**
     * 缓存键生成器
     */
    public static class CacheKeyGenerator {
        public static final String STRATEGY_KEY_PREFIX = "strategy:";
        public static final String CALCULATION_KEY_PREFIX = "calc:";
        public static final String DROOLS_RULE_KEY_PREFIX = "drools:";
        
        public static String generateStrategyKey(Long tenantId, String customerLevel, Long productCategoryId) {
            return STRATEGY_KEY_PREFIX + tenantId + ":" + customerLevel + ":" + productCategoryId;
        }
        
        public static String generateStrategyKey(Long strategyId) {
            return STRATEGY_KEY_PREFIX + strategyId;
        }
        
        public static String generateCalculationKey(String calculationId) {
            return CALCULATION_KEY_PREFIX + calculationId;
        }
        
        public static String generateCalculationKey(Long tenantId, Long productId, Long customerId, Integer quantity) {
            return CALCULATION_KEY_PREFIX + tenantId + ":" + productId + ":" + customerId + ":" + quantity;
        }
        
        public static String generateDroolsRuleKey(String ruleName) {
            return DROOLS_RULE_KEY_PREFIX + ruleName;
        }
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用GenericJackson2JsonRedisSerializer来序列化和反序列化对象
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = 
            new GenericJackson2JsonRedisSerializer();
        
        // 设置key和hashKey的序列化规则
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置value和hashValue的序列化规则
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))  // 默认TTL: 10分钟
                .disableCachingNullValues()        // 不缓存null值
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        // 为不同缓存设置不同的TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 价格策略缓存 - TTL: 10分钟
        cacheConfigurations.put(CacheNames.PRICE_STRATEGIES,
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 价格计算结果缓存 - TTL: 5分钟（较短，因为价格可能变化）
        cacheConfigurations.put(CacheNames.PRICE_CALCULATIONS,
                defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Drools规则缓存 - TTL: 30分钟（规则相对稳定）
        cacheConfigurations.put(CacheNames.DROOLS_RULES,
                defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 客户等级缓存 - TTL: 1小时（基础数据变化较少）
        cacheConfigurations.put(CacheNames.CUSTOMER_LEVELS,
                defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 产品类别缓存 - TTL: 1小时
        cacheConfigurations.put(CacheNames.PRODUCT_CATEGORIES,
                defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 租户配置缓存 - TTL: 2小时
        cacheConfigurations.put(CacheNames.TENANT_CONFIGS,
                defaultCacheConfig.entryTtl(Duration.ofHours(2)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
    
    /**
     * 本地缓存配置（Caffeine）作为一级缓存
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> localCache() {
        return com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(1000)                     // 最大缓存条目数
                .expireAfterWrite(Duration.ofMinutes(1)) // 写入后1分钟过期
                .recordStats()                         // 记录缓存统计信息
                .build();
    }
    
    /**
     * 缓存监控配置
     */
    @Bean
    public CacheMetrics cacheMetrics() {
        return new CacheMetrics();
    }
    
    /**
     * 缓存监控类
     */
    public static class CacheMetrics {
        private final com.github.benmanes.caffeine.cache.stats.CacheStats localCacheStats;
        
        public CacheMetrics() {
            this.localCacheStats = com.github.benmanes.caffeine.cache.stats.CacheStats.empty();
        }
        
        public void recordLocalCacheHit() {
            // 记录本地缓存命中
        }
        
        public void recordLocalCacheMiss() {
            // 记录本地缓存未命中
        }
        
        public void recordRedisCacheHit() {
            // 记录Redis缓存命中
        }
        
        public void recordRedisCacheMiss() {
            // 记录Redis缓存未命中
        }
        
        public Map<String, Object> getCacheStats() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("localCacheHitRate", localCacheStats.hitRate());
            stats.put("localCacheMissRate", localCacheStats.missRate());
            stats.put("localCacheRequestCount", localCacheStats.requestCount());
            stats.put("localCacheHitCount", localCacheStats.hitCount());
            stats.put("localCacheMissCount", localCacheStats.missCount());
            return stats;
        }
    }
    
    /**
     * 缓存工具类
     */
    public static class CacheUtils {
        private static final ThreadLocal<Boolean> BATCH_MODE = ThreadLocal.withInitial(() -> false);
        private static final ThreadLocal<Map<String, Object>> BATCH_CACHE = ThreadLocal.withInitial(HashMap::new);
        
        /**
         * 开启批量缓存模式
         */
        public static void startBatchMode() {
            BATCH_MODE.set(true);
            BATCH_CACHE.get().clear();
        }
        
        /**
         * 结束批量缓存模式，清空批量缓存
         */
        public static void endBatchMode() {
            BATCH_MODE.set(false);
            BATCH_CACHE.get().clear();
        }
        
        /**
         * 是否处于批量缓存模式
         */
        public static boolean isBatchMode() {
            return BATCH_MODE.get();
        }
        
        /**
         * 获取批量缓存中的数据
         */
        public static Object getFromBatchCache(String key) {
            return BATCH_CACHE.get().get(key);
        }
        
        /**
         * 放入批量缓存
         */
        public static void putInBatchCache(String key, Object value) {
            BATCH_CACHE.get().put(key, value);
        }
        
        /**
         * 生成带租户的缓存键
         */
        public static String withTenant(String baseKey, Long tenantId) {
            if (tenantId == null) {
                return baseKey;
            }
            return tenantId + ":" + baseKey;
        }
        
        /**
         * 生成带版本号的缓存键（用于缓存失效）
         */
        public static String withVersion(String baseKey, String version) {
            return baseKey + ":" + version;
        }
    }
}