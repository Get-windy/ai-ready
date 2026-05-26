package cn.aiedge.cache.util;

import cn.aiedge.cache.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 缓存策略工具类
 * 提供统一的缓存操作接口，支持多种缓存模式
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheStrategyUtil {

    private final CacheAsideSyncService cacheAsideService;
    private final WriteThroughCacheService writeThroughService;
    private final CacheProtectionService cacheProtectionService;

    /**
     * 使用Cache Aside模式获取数据
     */
    public <T> T getWithCacheAside(String cacheKey, Class<T> clazz, Supplier<T> dataLoader) {
        return getWithCacheAside(cacheKey, clazz, dataLoader, 3600L);
    }

    /**
     * 使用Cache Aside模式获取数据（带TTL）
     */
    public <T> T getWithCacheAside(String cacheKey, Class<T> clazz, Supplier<T> dataLoader, long ttlSeconds) {
        return cacheAsideService.getFromCache(cacheKey, clazz, dataLoader, ttlSeconds);
    }

    /**
     * 使用Cache Aside模式获取数据（带缓存保护）
     */
    public <T> T getWithCacheAsideProtected(String cacheKey, Class<T> clazz, Supplier<T> dataLoader, 
                                          long ttlSeconds, boolean enableBreakdownProtection) {
        if (enableBreakdownProtection) {
            return cacheProtectionService.handleCacheBreakdown(cacheKey, dataLoader, clazz, ttlSeconds);
        } else {
            return cacheProtectionService.handleCacheavalanche(cacheKey, dataLoader, clazz, ttlSeconds, 300L);
        }
    }

    /**
     * 使用Write Through模式写入数据
     */
    public <T> T writeWithWriteThrough(String cacheKey, T data, Function<T, T> databaseWriter) {
        return writeWithWriteThrough(cacheKey, data, databaseWriter, 3600L);
    }

    /**
     * 使用Write Through模式写入数据（带TTL）
     */
    public <T> T writeWithWriteThrough(String cacheKey, T data, Function<T, T> databaseWriter, long ttlSeconds) {
        return writeThroughService.writeThrough(cacheKey, data, databaseWriter, ttlSeconds);
    }

    /**
     * 使用Write Behind模式异步写入数据
     */
    public <T> CompletableFuture<T> writeWithWriteBehind(String cacheKey, T data, 
                                                       Function<T, T> databaseWriter) {
        return writeWithWriteBehind(cacheKey, data, databaseWriter, 3600L, 0L);
    }

    /**
     * 使用Write Behind模式异步写入数据（带TTL和延迟）
     */
    public <T> CompletableFuture<T> writeWithWriteBehind(String cacheKey, T data, 
                                                       Function<T, T> databaseWriter, 
                                                       long ttlSeconds, long delayMillis) {
        return writeThroughService.writeBehind(cacheKey, data, databaseWriter, ttlSeconds, delayMillis);
    }

    /**
     * 处理缓存穿透
     */
    public <T> T handleCachePenetration(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, long ttlSeconds) {
        return cacheProtectionService.handleCachePenetration(cacheKey, dataLoader, clazz, ttlSeconds);
    }

    /**
     * 处理缓存击穿
     */
    public <T> T handleCacheBreakdown(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, long ttlSeconds) {
        return cacheProtectionService.handleCacheBreakdown(cacheKey, dataLoader, clazz, ttlSeconds);
    }

    /**
     * 处理缓存雪崩
     */
    public <T> T handleCacheavalanche(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, 
                                    long baseTtlSeconds, long varianceSeconds) {
        return cacheProtectionService.handleCacheavalanche(cacheKey, dataLoader, clazz, baseTtlSeconds, varianceSeconds);
    }

    /**
     * 通用缓存获取方法，根据策略自动选择最佳方式
     */
    public <T> T getFromCache(String cacheKey, Class<T> clazz, Supplier<T> dataLoader, 
                            CacheStrategy strategy, long ttlSeconds) {
        switch (strategy) {
            case CACHE_ASIDE:
                return getWithCacheAside(cacheKey, clazz, dataLoader, ttlSeconds);
            case CACHE_ASIDE_PROTECTED:
                return getWithCacheAsideProtected(cacheKey, clazz, dataLoader, ttlSeconds, true);
            case CACHE_PENETRATION_PROTECTION:
                return handleCachePenetration(cacheKey, dataLoader, clazz, ttlSeconds);
            case CACHE_BREAKDOWN_PROTECTION:
                return handleCacheBreakdown(cacheKey, dataLoader, clazz, ttlSeconds);
            case CACHE_AVALANCHE_PROTECTION:
                return handleCacheavalanche(cacheKey, dataLoader, clazz, ttlSeconds, 300L);
            default:
                return getWithCacheAside(cacheKey, clazz, dataLoader, ttlSeconds);
        }
    }

    /**
     * 通用缓存写入方法，根据策略自动选择最佳方式
     */
    public <T> T writeToCache(String cacheKey, T data, Function<T, T> databaseWriter, 
                            WriteStrategy writeStrategy, long ttlSeconds) {
        switch (writeStrategy) {
            case WRITE_THROUGH:
                return writeWithWriteThrough(cacheKey, data, databaseWriter, ttlSeconds);
            case WRITE_BEHIND_ASYNC:
                try {
                    return writeWithWriteBehind(cacheKey, data, databaseWriter, ttlSeconds, 0L).join();
                } catch (Exception e) {
                    log.error("Write Behind操作失败，回退到Write Through", e);
                    return writeWithWriteThrough(cacheKey, data, databaseWriter, ttlSeconds);
                }
            default:
                return writeWithWriteThrough(cacheKey, data, databaseWriter, ttlSeconds);
        }
    }

    /**
     * 缓存策略枚举
     */
    public enum CacheStrategy {
        CACHE_ASIDE,                    // 基础Cache Aside模式
        CACHE_ASIDE_PROTECTED,          // 带保护的Cache Aside模式
        CACHE_PENETRATION_PROTECTION,   // 缓存穿透保护
        CACHE_BREAKDOWN_PROTECTION,     // 缓存击穿保护
        CACHE_AVALANCHE_PROTECTION      // 缓存雪崩保护
    }

    /**
     * 写入策略枚举
     */
    public enum WriteStrategy {
        WRITE_THROUGH,      // Write Through模式
        WRITE_BEHIND_ASYNC  // Write Behind异步模式
    }
}