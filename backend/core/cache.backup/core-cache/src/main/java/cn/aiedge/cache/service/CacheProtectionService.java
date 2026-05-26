package cn.aiedge.cache.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存保护服务
 * 处理缓存穿透、击穿、雪崩等常见问题
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class CacheProtectionService {
    
    private static final Logger log = LoggerFactory.getLogger(CacheProtectionService.class);

    private final StringRedisTemplate redisTemplate;
    private final CacheSyncService cacheSyncService;
    private final Executor cacheExecutor;

    /**
     * 处理缓存穿透
     * 缓存穿透是指查询一个不存在的数据，由于缓存中没有，每次都会访问数据库
     * 解决方案：缓存空结果或使用布隆过滤器
     */
    public <T> T handleCachePenetration(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, long ttlSeconds) {
        // 1. 先查缓存
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                T result = JSONUtil.toBean(cachedValue, clazz);
                // 检查是否是空值标识
                if (result != null && !isEmptyValue(result)) {
                    log.debug("缓存命中（非空值）: key={}", cacheKey);
                    return result;
                } else if (result != null) {
                    log.debug("缓存命中（空值标识）: key={}", cacheKey);
                    return null; // 返回null表示确实没有数据
                }
            } catch (Exception e) {
                log.warn("缓存数据解析失败: key={}, error={}", cacheKey, e.getMessage());
            }
        }

        // 2. 缓存未命中，查询数据库
        log.debug("缓存未命中，查询数据库: key={}", cacheKey);
        T result = dataLoader.get();

        // 3. 根据查询结果决定缓存策略
        if (result != null) {
            // 查询到数据，正常缓存
            String jsonValue = JSONUtil.toJsonStr(result);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, ttlSeconds, TimeUnit.SECONDS);
            log.debug("缓存穿透处理 - 数据存在，写入缓存: key={}", cacheKey);
        } else {
            // 未查询到数据，缓存空值标识，避免频繁查询不存在的数据
            // 使用特殊空值标识，如JSON字符串 "{}"
            String emptyValue = "{}";
            // 空值缓存时间较短，避免长期占用缓存
            long emptyTtl = Math.min(ttlSeconds, 300L); // 最多5分钟
            redisTemplate.opsForValue().set(cacheKey, emptyValue, emptyTtl, TimeUnit.SECONDS);
            log.debug("缓存穿透处理 - 数据不存在，缓存空值: key={}, ttl={}s", cacheKey, emptyTtl);
        }

        return result;
    }

    /**
     * 处理缓存击穿
     * 缓存击穿是指热点数据在缓存过期的瞬间，大量请求同时涌入数据库
     * 解决方案：分布式锁 + 双重检查
     */
    public <T> T handleCacheBreakdown(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, long ttlSeconds) {
        // 1. 先查缓存
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                T result = JSONUtil.toBean(cachedValue, clazz);
                if (result != null && !isEmptyValue(result)) {
                    log.debug("缓存命中: key={}", cacheKey);
                    return result;
                }
            } catch (Exception e) {
                log.warn("缓存数据解析失败: key={}, error={}", cacheKey, e.getMessage());
            }
        }

        // 2. 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = "lock:breakdown:" + cacheKey;
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
                        try {
                            T result = JSONUtil.toBean(doubleCheckValue, clazz);
                            if (result != null && !isEmptyValue(result)) {
                                log.debug("双重检查缓存命中: key={}", cacheKey);
                                return result;
                            }
                        } catch (Exception e) {
                            log.warn("双重检查缓存数据解析失败: key={}, error={}", cacheKey, e.getMessage());
                        }
                    }

                    // 缓存仍然未命中，查询数据库
                    log.debug("获取锁后查询数据库: key={}", cacheKey);
                    T result = dataLoader.get();

                    // 将结果写入缓存
                    if (result != null) {
                        String jsonValue = JSONUtil.toJsonStr(result);
                        redisTemplate.opsForValue().set(cacheKey, jsonValue, ttlSeconds, TimeUnit.SECONDS);
                        log.debug("缓存击穿处理 - 写入缓存: key={}", cacheKey);
                    } else {
                        // 查询无结果，缓存空值
                        String emptyValue = "{}";
                        long emptyTtl = Math.min(ttlSeconds, 300L);
                        redisTemplate.opsForValue().set(cacheKey, emptyValue, emptyTtl, TimeUnit.SECONDS);
                        log.debug("缓存击穿处理 - 缓存空值: key={}", cacheKey);
                    }

                    return result;
                } finally {
                    // 释放锁
                    releaseDistributedLock(lockKey, lockValue);
                }
            } else {
                // 获取锁失败，等待一小段时间后重试（最多重试一次）
                log.debug("获取缓存击穿防护锁失败，稍后重试: key={}", cacheKey);
                try {
                    Thread.sleep(RandomUtil.randomLong(10, 50)); // 随机等待10-50ms，避免惊群效应
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // 重试一次
                return handleCacheBreakdownOnceMore(cacheKey, dataLoader, clazz, ttlSeconds);
            }
        } catch (Exception e) {
            log.error("缓存击穿处理异常: key={}, error={}", cacheKey, e.getMessage(), e);
            // 发生异常时直接查询数据库，不使用缓存
            return dataLoader.get();
        }
    }

    /**
     * 缓存击穿处理 - 仅重试一次
     */
    private <T> T handleCacheBreakdownOnceMore(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, long ttlSeconds) {
        // 再次检查缓存
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                T result = JSONUtil.toBean(cachedValue, clazz);
                if (result != null && !isEmptyValue(result)) {
                    log.debug("重试后缓存命中: key={}", cacheKey);
                    return result;
                }
            } catch (Exception e) {
                log.warn("重试后缓存数据解析失败: key={}, error={}", cacheKey, e.getMessage());
            }
        }

        // 再次查询数据库
        log.debug("重试后查询数据库: key={}", cacheKey);
        return dataLoader.get();
    }

    /**
     * 处理缓存雪崩
     * 缓存雪崩是指大量缓存在同一时间过期，导致大量请求直接打到数据库
     * 解决方案：设置不同的过期时间、多级缓存、熔断降级
     */
    public <T> T handleCacheavalanche(String baseCacheKey, Supplier<T> dataLoader, Class<T> clazz, 
                                     long baseTtlSeconds, long varianceSeconds) {
        // 在基础TTL上增加随机偏差，避免同时过期
        long actualTtl = baseTtlSeconds + RandomUtil.randomLong(-varianceSeconds, varianceSeconds);
        // 确保TTL不会小于最小值
        actualTtl = Math.max(actualTtl, 60); // 最少60秒

        // 1. 先查缓存
        String cachedValue = redisTemplate.opsForValue().get(baseCacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                T result = JSONUtil.toBean(cachedValue, clazz);
                if (result != null && !isEmptyValue(result)) {
                    log.debug("缓存命中: key={}", baseCacheKey);
                    return result;
                }
            } catch (Exception e) {
                log.warn("缓存数据解析失败: key={}, error={}", baseCacheKey, e.getMessage());
            }
        }

        // 2. 缓存未命中，查询数据库
        log.debug("缓存未命中，查询数据库: key={}", baseCacheKey);
        T result = dataLoader.get();

        // 3. 将结果写入缓存，使用随机TTL
        if (result != null) {
            String jsonValue = JSONUtil.toJsonStr(result);
            redisTemplate.opsForValue().set(baseCacheKey, jsonValue, actualTtl, TimeUnit.SECONDS);
            log.debug("缓存雪崩处理 - 写入缓存: key={}, ttl={}s", baseCacheKey, actualTtl);
        } else {
            // 查询无结果，缓存空值
            String emptyValue = "{}";
            long emptyTtl = Math.min(actualTtl, 300L);
            redisTemplate.opsForValue().set(baseCacheKey, emptyValue, emptyTtl, TimeUnit.SECONDS);
            log.debug("缓存雪崩处理 - 缓存空值: key={}", baseCacheKey);
        }

        return result;
    }

    /**
     * 多级缓存处理 - 结合缓存穿透、击穿、雪崩的综合处理
     */
    public <T> T handleMultiLevelCache(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, 
                                      long ttlSeconds, boolean enableBreakdownProtection) {
        if (enableBreakdownProtection) {
            return handleCacheBreakdown(cacheKey, dataLoader, clazz, ttlSeconds);
        } else {
            return handleCacheavalanche(cacheKey, dataLoader, clazz, ttlSeconds, 300L); // 随机偏差5分钟
        }
    }

    /**
     * 批量处理缓存穿透（使用布隆过滤器思路）
     */
    public <T> java.util.Map<String, T> handleBatchCachePenetration(List<String> cacheKeys, 
                                                                  Function<List<String>, java.util.Map<String, T>> batchDataLoader,
                                                                  Class<T> clazz, long ttlSeconds) {
        java.util.Map<String, T> results = new java.util.HashMap<>();
        
        // 分离已存在的缓存和需要查询的键
        for (String key : cacheKeys) {
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(cachedValue)) {
                try {
                    T result = JSONUtil.toBean(cachedValue, clazz);
                    if (result != null && !isEmptyValue(result)) {
                        results.put(key, result);
                        log.debug("批量缓存命中: key={}", key);
                    } else {
                        results.put(key, null); // 空值
                    }
                } catch (Exception e) {
                    log.warn("批量缓存数据解析失败: key={}, error={}", key, e.getMessage());
                }
            }
        }

        // 获取未命中的键
        List<String> missedKeys = cacheKeys.stream()
            .filter(key -> !results.containsKey(key))
            .collect(java.util.stream.Collectors.toList());

        if (CollUtil.isNotEmpty(missedKeys)) {
            // 批量查询数据库
            java.util.Map<String, T> dbResults = batchDataLoader.apply(missedKeys);

            // 处理查询结果
            for (String key : missedKeys) {
                T result = dbResults != null ? dbResults.get(key) : null;
                
                if (result != null) {
                    // 查询到数据，缓存
                    String jsonValue = JSONUtil.toJsonStr(result);
                    redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
                    results.put(key, result);
                    log.debug("批量缓存穿透处理 - 数据存在，写入缓存: key={}", key);
                } else {
                    // 未查询到数据，缓存空值
                    String emptyValue = "{}";
                    long emptyTtl = Math.min(ttlSeconds, 300L);
                    redisTemplate.opsForValue().set(key, emptyValue, emptyTtl, TimeUnit.SECONDS);
                    results.put(key, null);
                    log.debug("批量缓存穿透处理 - 数据不存在，缓存空值: key={}", key);
                }
            }
        }

        return results;
    }

    /**
     * 检查是否为空值标识
     */
    private <T> boolean isEmptyValue(T result) {
        if (result == null) {
            return true;
        }
        
        // 检查是否是空对象标识 "{}"
        if (result instanceof String) {
            return "{}".equals(result);
        }
        
        // 对于其他类型的对象，可以根据实际情况判断
        return false;
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
     * 异步处理缓存穿透
     */
    public <T> CompletableFuture<T> handleCachePenetrationAsync(String cacheKey, Supplier<T> dataLoader, 
                                                              Class<T> clazz, long ttlSeconds) {
        return CompletableFuture.supplyAsync(() -> 
            handleCachePenetration(cacheKey, dataLoader, clazz, ttlSeconds), cacheExecutor);
    }

    /**
     * 异步处理缓存击穿
     */
    public <T> CompletableFuture<T> handleCacheBreakdownAsync(String cacheKey, Supplier<T> dataLoader, 
                                                            Class<T> clazz, long ttlSeconds) {
        return CompletableFuture.supplyAsync(() -> 
            handleCacheBreakdown(cacheKey, dataLoader, clazz, ttlSeconds), cacheExecutor);
    }

    /**
     * 异步处理缓存雪崩
     */
    public <T> CompletableFuture<T> handleCacheavalancheAsync(String baseCacheKey, Supplier<T> dataLoader, 
                                                            Class<T> clazz, long baseTtlSeconds, long varianceSeconds) {
        return CompletableFuture.supplyAsync(() -> 
            handleCacheavalanche(baseCacheKey, dataLoader, clazz, baseTtlSeconds, varianceSeconds), cacheExecutor);
    }
}