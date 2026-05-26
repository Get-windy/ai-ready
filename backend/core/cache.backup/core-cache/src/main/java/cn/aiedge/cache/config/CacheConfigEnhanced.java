package cn.aiedge.cache.config;

import cn.aiedge.cache.service.CacheWarmupService;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置增强类
 * 提供更精细化的缓存管理配置
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@EnableAsync
public class CacheConfigEnhanced {
    
    @Bean(name = "cacheExecutor")
    public Executor cacheExecutor() {
        return Executors.newFixedThreadPool(4, r -> new Thread(r, "cache-worker"));
    }
    
    /**
     * 缓存属性配置
     */
    @Data
    @ConfigurationProperties(prefix = "app.cache")
    public static class CacheProperties {
        
        private Warmup warmup = new Warmup();
        private Eviction eviction = new Eviction();
        private Local local = new Local();
        
        @Data
        public static class Warmup {
            private boolean enabled = true;
            private String startupDelay = "30s";
            private int batchSize = 100;
            private int threadPoolSize = 4;
        }
        
        @Data
        public static class Eviction {
            private Config user = new Config(300, 10);
            private Config permission = new Config(7200, 5);
            private Config config = new Config(86400, 15);
            private Config dict = new Config(43200, 10);
            private Config stock = new Config(300, 5);
            
            @Data
            public static class Config {
                private long ttlSeconds;
                private long delaySeconds;
                
                public Config(long ttlSeconds, long delaySeconds) {
                    this.ttlSeconds = ttlSeconds;
                    this.delaySeconds = delaySeconds;
                }
            }
        }
        
        @Data
        public static class Local {
            private Config defaultCache = new Config(10000, 600, 300);
            private Config userCache = new Config(5000, 7200, 3600);
            private Config stockCache = new Config(50000, 300, 120);
            
            @Data
            public static class Config {
                private long maxSize;
                private long expireWriteSeconds;
                private long expireAccessSeconds;
                
                public Config(long maxSize, long expireWriteSeconds, long expireAccessSeconds) {
                    this.maxSize = maxSize;
                    this.expireWriteSeconds = expireWriteSeconds;
                    this.expireAccessSeconds = expireAccessSeconds;
                }
            }
        }
    }
    
    /**
     * 创建缓存预热服务
     */
    @Bean
    public CacheWarmupService cacheWarmupService() {
        return new CacheWarmupService();
    }
    
    /**
     * 用户权限缓存管理器
     */
    @Bean("userPermissionCacheManager")
    public CacheManager userPermissionCacheManager(CacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(properties.getLocal().getUserCache().getMaxSize())
                .expireAfterWrite(properties.getLocal().getUserCache().getExpireWriteSeconds(), TimeUnit.SECONDS)
                .expireAfterAccess(properties.getLocal().getUserCache().getExpireAccessSeconds(), TimeUnit.SECONDS)
                .recordStats());
        return cacheManager;
    }
    
    /**
     * 库存数据缓存管理器
     */
    @Bean("stockCacheManager")
    public CacheManager stockCacheManager(CacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(properties.getLocal().getStockCache().getMaxSize())
                .expireAfterWrite(properties.getLocal().getStockCache().getExpireWriteSeconds(), TimeUnit.SECONDS)
                .expireAfterAccess(properties.getLocal().getStockCache().getExpireAccessSeconds(), TimeUnit.SECONDS)
                .recordStats());
        return cacheManager;
    }
}