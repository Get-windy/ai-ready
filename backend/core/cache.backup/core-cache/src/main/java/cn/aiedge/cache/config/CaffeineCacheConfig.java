package cn.aiedge.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存配置
 * 提供高性能的本地内存缓存
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Configuration
public class CaffeineCacheConfig {

    /**
     * 默认缓存配置
     * - 最大容量：10000
     * - 写入过期：10分钟
     * - 访问过期：5分钟
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> 
                    log.debug("Caffeine缓存移除: key={}, cause={}", key, cause));
    }

    /**
     * Caffeine缓存管理器（Primary）
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        cacheManager.setAllowNullValues(true);
        log.info("Caffeine缓存管理器初始化完成");
        return cacheManager;
    }

    /**
     * 用户权限缓存配置
     * - 最大容量：5000
     * - 写入过期：2小时
     */
    @Bean("userPermissionCache")
    public CacheManager userPermissionCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("userPermission");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }

    /**
     * 系统配置缓存配置
     * - 最大容量：1000
     * - 写入过期：24小时
     */
    @Bean("sysConfigCache")
    public CacheManager sysConfigCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("sysConfig");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }

    /**
     * 字典数据缓存配置
     * - 最大容量：2000
     * - 写入过期：12小时
     */
    @Bean("dictCache")
    public CacheManager dictCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("dict");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }

    /**
     * 库存数据缓存配置
     * - 最大容量：50000
     * - 写入过期：5分钟
     * - 访问过期：2分钟
     */
    @Bean("stockCache")
    public CacheManager stockCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("stock");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(50000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}
