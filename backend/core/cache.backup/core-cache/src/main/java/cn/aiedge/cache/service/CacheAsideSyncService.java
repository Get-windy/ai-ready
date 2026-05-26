package cn.aiedge.cache.service;

import cn.aiedge.cache.annotation.MultiLevelCache;
import cn.aiedge.cache.annotation.MultiLevelCacheEvict;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Cache Aside模式缓存同步服务
 * 实现Cache Aside模式的数据同步机制
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheAsideSyncService {

    private final StringRedisTemplate redisTemplate;
    private final CacheSyncService cacheSyncService;
    private final Executor cacheExecutor;

    /**
     * 初始化缓存同步监听器
     */
    @PostConstruct
    public void init() {
        // 订阅缓存同步事件
        cacheSyncService.subscribe("cache-aside", syncMessage -> {
            String key = syncMessage.getKey();
            CacheSyncService.SyncAction action = syncMessage.getAction();
            
            switch (action) {
                case EVICT:
                    // 清除本地缓存
                    clearLocalCache(key);
                    break;
                case UPDATE:
                    // 更新本地缓存（可选）
                    log.debug("收到缓存更新同步指令: key={}", key);
                    break;
                case CLEAR_ALL:
                    // 清除所有本地缓存
                    clearAllLocalCache();
                    break;
            }
        });
        
        log.info("Cache Aside同步服务初始化完成");
    }

    /**
     * Cache Aside模式 - 从缓存获取数据
     * 先查缓存，缓存未命中再查数据库，然后回填缓存
     */
    public <T> T getFromCache(String cacheKey, Class<T> clazz, java.util.function.Supplier<T> dataLoader) {
        return getFromCache(cacheKey, clazz, dataLoader, 3600); // 默认TTL为1小时
    }

    /**
     * Cache Aside模式 - 从缓存获取数据（带TTL设置）
     */
    public <T> T getFromCache(String cacheKey, Class<T> clazz, java.util.function.Supplier<T> dataLoader, long ttlSeconds) {
        // 1. 先从Redis缓存获取
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                T result = JSONUtil.toBean(cachedValue, clazz);
                log.debug("缓存命中: key={}", cacheKey);
                return result;
            } catch (Exception e) {
                log.warn("缓存数据解析失败: key={}, error={}", cacheKey, e.getMessage());
                // 解析失败则删除无效缓存
                evictCache(cacheKey);
            }
        }

        // 2. 缓存未命中，查询数据库
        log.debug("缓存未命中，查询数据库: key={}", cacheKey);
        
        // 防止缓存击穿：使用分布式锁
        String lockKey = "lock:cache_miss:" + cacheKey;
        String lockValue = System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        long lockExpireTime = 30; // 锁过期时间30秒
        
        try {
            // 尝试获取分布式锁
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey, lockValue, lockExpireTime, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                try {
                    // 双重检查：再次尝试从缓存获取（可能其他线程刚填充了缓存）
                    String doubleCheckValue = redisTemplate.opsForValue().get(cacheKey);
                    if (StrUtil.isNotBlank(doubleCheckValue)) {
                        return JSONUtil.toBean(doubleCheckValue, clazz);
                    }
                    
                    // 查询数据库
                    T result = dataLoader.get();
                    
                    // 3. 将结果写入缓存
                    if (result != null) {
                        writeToCache(cacheKey, result, ttlSeconds);
                    } else {
                        // 处理缓存穿透：数据库也查不到数据，缓存空值
                        handleCachePenetration(cacheKey, ttlSeconds);
                    }
                    
                    return result;
                } finally {
                    // 释放锁
                    releaseDistributedLock(lockKey, lockValue);
                }
            } else {
                // 获取锁失败，等待一段时间后重试
                log.debug("获取缓存锁失败，等待重试: key={}", cacheKey);
                try {
                    Thread.sleep(50); // 等待50ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                // 递归调用，但最多递归一次
                return getFromCacheOnceMore(cacheKey, clazz, dataLoader, ttlSeconds);
            }
        } catch (Exception e) {
            log.error("缓存操作异常: key={}, error={}", cacheKey, e.getMessage(), e);
            // 发生异常时直接查询数据库，不使用缓存
            return dataLoader.get();
        }
    }

    /**
     * 仅尝试一次获取缓存（防止无限递归）
     */
    private <T> T getFromCacheOnceMore(String cacheKey, Class<T> clazz, java.util.function.Supplier<T> dataLoader, long ttlSeconds) {
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                return JSONUtil.toBean(cachedValue, clazz);
            } catch (Exception e) {
                log.warn("缓存数据解析失败: key={}, error={}", cacheKey, e.getMessage());
                return dataLoader.get();
            }
        }
        return dataLoader.get();
    }

    /**
     * Cache Aside模式 - 写入缓存
     */
    public <T> void writeToCache(String cacheKey, T data, long ttlSeconds) {
        if (data == null) {
            log.warn("不能写入null数据到缓存: key={}", cacheKey);
            return;
        }
        
        try {
            String jsonValue = JSONUtil.toJsonStr(data);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("写入缓存成功: key={}, ttl={}s", cacheKey, ttlSeconds);
        } catch (Exception e) {
            log.error("写入缓存失败: key={}, error={}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Cache Aside模式 - 删除缓存
     */
    public void evictCache(String cacheKey) {
        if (StrUtil.isBlank(cacheKey)) {
            return;
        }
        
        try {
            redisTemplate.delete(cacheKey);
            
            // 发布缓存同步消息，确保集群中其他节点也清除缓存
            cacheSyncService.publishSync("cache-aside", cacheKey, CacheSyncService.SyncAction.EVICT);
            
            log.debug("清除缓存成功: key={}", cacheKey);
        } catch (Exception e) {
            log.error("清除缓存失败: key={}, error={}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Cache Aside模式 - 批量删除缓存
     */
    public void evictCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (CollectionUtil.isNotEmpty(keys)) {
                redisTemplate.delete(keys);
                
                // 发布批量清除同步消息
                for (String key : keys) {
                    cacheSyncService.publishSync("cache-aside", key, CacheSyncService.SyncAction.EVICT);
                }
                
                log.debug("批量清除缓存成功: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("批量清除缓存失败: pattern={}, error={}", pattern, e.getMessage(), e);
        }
    }

    /**
     * 处理缓存穿透 - 缓存空值
     */
    private void handleCachePenetration(String cacheKey, long ttlSeconds) {
        // 缓存一个空字符串或特殊标识，避免频繁查询不存在的数据
        // TTL设置短一些，比如5-10分钟
        long nullCacheTtl = Math.min(ttlSeconds, 300L); // 最大5分钟
        redisTemplate.opsForValue().set(cacheKey, "{}", nullCacheTtl, TimeUnit.SECONDS);
        
        log.debug("缓存穿透处理：缓存空值: key={}, ttl={}s", cacheKey, nullCacheTtl);
    }

    /**
     * 清除本地缓存（由同步消息触发）
     */
    private void clearLocalCache(String cacheKey) {
        log.debug("清除本地缓存: key={}", cacheKey);
        // 在实际应用中，这里应该清除本地进程缓存
        // 比如清除Caffeine本地缓存中的对应项
    }

    /**
     * 清除所有本地缓存（由同步消息触发）
     */
    private void clearAllLocalCache() {
        log.debug("清除所有本地缓存");
        // 清除所有本地缓存
    }

    /**
     * 释放分布式锁
     */
    private void releaseDistributedLock(String lockKey, String lockValue) {
        try {
            // 使用Lua脚本保证原子性地检查和删除锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long result = redisTemplate.execute(
                connection -> connection.eval(script.getBytes(), 1, lockKey.getBytes(), lockValue.getBytes()),
                String.class, lockKey, lockValue);
            
            if (result != null && result == 1L) {
                log.debug("成功释放分布式锁: key={}", lockKey);
            } else {
                log.debug("未能释放分布式锁（可能已过期）: key={}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}, error={}", lockKey, e.getMessage(), e);
        }
    }

    /**
     * 异步写入缓存
     */
    public <T> CompletableFuture<Void> writeToCacheAsync(String cacheKey, T data, long ttlSeconds) {
        return CompletableFuture.runAsync(() -> writeToCache(cacheKey, data, ttlSeconds), cacheExecutor);
    }

    /**
     * 异步删除缓存
     */
    public CompletableFuture<Void> evictCacheAsync(String cacheKey) {
        return CompletableFuture.runAsync(() -> evictCache(cacheKey), cacheExecutor);
    }
}