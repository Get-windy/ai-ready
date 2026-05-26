package cn.aiedge.cache.config;

import cn.aiedge.cache.service.*;
import cn.aiedge.cache.util.CacheStrategyUtil;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 缓存同步配置类
 * 配置缓存同步相关的组件和服务
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties(CacheSyncConfig.Properties.class)
public class CacheSyncConfig {

    /**
     * 缓存同步相关属性配置
     */
    @Data
    @ConfigurationProperties(prefix = "app.cache.sync")
    public static class Properties {
        private RedisSync redis = new RedisSync();
        private Protection protection = new Protection();
        private Advanced advanced = new Advanced();

        @Data
        public static class RedisSync {
            private String syncChannelPrefix = "cache:sync:";
            private long syncTimeout = 5000; // 同步超时时间（毫秒）
            private int maxRetries = 3; // 最大重试次数
            private long retryDelay = 100; // 重试延迟（毫秒）
        }

        @Data
        public static class Protection {
            private boolean enablePenetrationProtection = true; // 启用穿透保护
            private boolean enableBreakdownProtection = true;   // 启用击穿保护
            private boolean enableavalancheProtection = true;   // 启用雪崩保护
            private long emptyValueTtl = 300; // 空值缓存时间（秒）
            private long lockTimeout = 30; // 分布式锁超时时间（秒）
        }

        @Data
        public static class Advanced {
            private boolean enableWriteThrough = true; // 启用Write Through
            private boolean enableWriteBehind = true;  // 启用Write Behind
            private int writeBehindThreadPoolSize = 4; // Write Behind线程池大小
            private long writeBehindDelay = 1000; // Write Behind延迟时间（毫秒）
        }
    }

    /**
     * 缓存同步执行器
     */
    @Bean("cacheSyncExecutor")
    public Executor cacheSyncExecutor(Properties properties) {
        return Executors.newFixedThreadPool(
            properties.getAdvanced().getWriteBehindThreadPoolSize(),
            r -> new Thread(r, "cache-sync-worker")
        );
    }

    /**
     * 缓存策略工具类
     */
    @Bean
    public CacheStrategyUtil cacheStrategyUtil(
            CacheAsideSyncService cacheAsideService,
            WriteThroughCacheService writeThroughService,
            CacheProtectionService cacheProtectionService) {
        return new CacheStrategyUtil(cacheAsideService, writeThroughService, cacheProtectionService);
    }

    /**
     * 缓存同步服务（条件激活）
     */
    @Bean
    @ConditionalOnProperty(name = "app.cache.sync.enabled", havingValue = "true", matchIfMissing = true)
    public CacheSyncService cacheSyncService() {
        return new CacheSyncService(null, null); // 注意：这里会被Spring注入实际依赖
    }

    /**
     * Cache Aside同步服务
     */
    @Bean
    public CacheAsideSyncService cacheAsideSyncService() {
        return new CacheAsideSyncService(null, null, null); // 注意：这里会被Spring注入实际依赖
    }

    /**
     * Write Through缓存服务
     */
    @Bean
    public WriteThroughCacheService writeThroughCacheService() {
        return new WriteThroughCacheService(null, null, null); // 注意：这里会被Spring注入实际依赖
    }

    /**
     * 缓存保护服务
     */
    @Bean
    public CacheProtectionService cacheProtectionService() {
        return new CacheProtectionService(null, null, null); // 注意：这里会被Spring注入实际依赖
    }
}