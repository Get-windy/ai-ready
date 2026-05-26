package cn.aiedge.cache.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Write Through模式缓存服务
 * 实现写入时同步更新缓存和数据库的机制
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class WriteThroughCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(WriteThroughCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final CacheSyncService cacheSyncService;
    private final Executor cacheExecutor;

    /**
     * Write Through模式 - 写入数据
     * 同时写入缓存和数据库，保持数据一致性
     */
    public <T> T writeThrough(String cacheKey, T data, Function<T, T> databaseWriter) {
        return writeThrough(cacheKey, data, databaseWriter, 3600); // 默认TTL为1小时
    }

    /**
     * Write Through模式 - 写入数据（带TTL设置）
     */
    public <T> T writeThrough(String cacheKey, T data, Function<T, T> databaseWriter, long ttlSeconds) {
        if (data == null) {
            throw new IllegalArgumentException("不能写入null数据");
        }

        try {
            // 1. 先写入数据库
            T result = databaseWriter.apply(data);
            
            if (result != null) {
                // 2. 写入成功后，同步更新缓存
                writeToCache(cacheKey, result, ttlSeconds);
                
                log.debug("Write Through写入成功: key={}", cacheKey);
            } else {
                log.warn("数据库写入返回null，不更新缓存: key={}", cacheKey);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Write Through写入失败: key={}, error={}", cacheKey, e.getMessage(), e);
            throw e; // 抛出异常，让上层处理
        }
    }

    /**
     * Write Through模式 - 批量写入数据
     */
    public <T> void writeThroughBatch(java.util.Map<String, T> cacheDataMap, 
                                    Function<java.util.Map<String, T>, java.util.Map<String, T>> databaseBatchWriter,
                                    long ttlSeconds) {
        try {
            // 1. 先批量写入数据库
            java.util.Map<String, T> results = databaseBatchWriter.apply(cacheDataMap);
            
            if (results != null && !results.isEmpty()) {
                // 2. 批量更新缓存
                for (String key : results.keySet()) {
                    T value = results.get(key);
                    if (value != null) {
                        writeToCache(key, value, ttlSeconds);
                    }
                }
                
                log.debug("Write Through批量写入成功: count={}", results.size());
            }
        } catch (Exception e) {
            log.error("Write Through批量写入失败: error={}", e.getMessage(), e);
            throw e; // 抛出异常，让上层处理
        }
    }

    /**
     * Write Through模式 - 删除数据
     * 同时删除缓存和数据库中的数据
     */
    public void deleteThrough(String cacheKey, Runnable databaseDeleter) {
        try {
            // 1. 先删除数据库中的数据
            databaseDeleter.run();
            
            // 2. 删除成功后，同步删除缓存
            evictCache(cacheKey);
            
            log.debug("Write Through删除完成: key={}", cacheKey);
        } catch (Exception e) {
            log.error("Write Through删除失败: key={}, error={}", cacheKey, e.getMessage(), e);
            throw e; // 抛出异常，让上层处理
        }
    }

    /**
     * 写入缓存
     */
    private <T> void writeToCache(String cacheKey, T data, long ttlSeconds) {
        try {
            String jsonValue = JSONUtil.toJsonStr(data);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, ttlSeconds, TimeUnit.SECONDS);
            
            // 发布缓存同步消息，确保集群中其他节点也更新缓存
            cacheSyncService.publishSync("write-through", cacheKey, CacheSyncService.SyncAction.UPDATE);
            
            log.debug("缓存更新成功: key={}, ttl={}s", cacheKey, ttlSeconds);
        } catch (Exception e) {
            log.error("缓存更新失败: key={}, error={}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * 删除缓存
     */
    private void evictCache(String cacheKey) {
        if (StrUtil.isBlank(cacheKey)) {
            return;
        }
        
        try {
            redisTemplate.delete(cacheKey);
            
            // 发布缓存同步消息，确保集群中其他节点也清除缓存
            cacheSyncService.publishSync("write-through", cacheKey, CacheSyncService.SyncAction.EVICT);
            
            log.debug("缓存删除完成: key={}", cacheKey);
        } catch (Exception e) {
            log.error("缓存删除失败: key={}, error={}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * 异步Write Through写入
     */
    public <T> CompletableFuture<T> writeThroughAsync(String cacheKey, T data, 
                                                    Function<T, T> databaseWriter, long ttlSeconds) {
        return CompletableFuture.supplyAsync(() -> writeThrough(cacheKey, data, databaseWriter, ttlSeconds), cacheExecutor);
    }

    /**
     * Write Behind模式 - 异步写入
     * 先更新缓存，然后异步更新数据库
     */
    public <T> CompletableFuture<T> writeBehind(String cacheKey, T data, 
                                              Function<T, T> databaseWriter, long ttlSeconds) {
        return writeBehind(cacheKey, data, databaseWriter, ttlSeconds, 0); // 立即执行
    }

    /**
     * Write Behind模式 - 带延迟的异步写入
     */
    public <T> CompletableFuture<T> writeBehind(String cacheKey, T data, 
                                              Function<T, T> databaseWriter, long ttlSeconds, long delayMillis) {
        try {
            // 1. 立即更新缓存
            writeToCache(cacheKey, data, ttlSeconds);
            
            log.debug("Write Behind: 缓存已更新，等待异步写入数据库: key={}", cacheKey);
            
            // 2. 异步延迟更新数据库
            CompletableFuture<T> future = new CompletableFuture<>();
            
            CompletableFuture.runAsync(() -> {
                try {
                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);
                    }
                    
                    T result = databaseWriter.apply(data);
                    
                    if (result != null) {
                        // 数据库更新成功后，再次更新缓存（确保一致性）
                        writeToCache(cacheKey, result, ttlSeconds);
                        
                        log.debug("Write Behind: 数据库更新完成: key={}", cacheKey);
                    }
                    
                    future.complete(result);
                } catch (Exception e) {
                    log.error("Write Behind: 数据库更新失败: key={}, error={}", cacheKey, e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }, cacheExecutor);
            
            return future;
        } catch (Exception e) {
            log.error("Write Behind初始化失败: key={}, error={}", cacheKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量Write Behind模式
     */
    public CompletableFuture<Void> writeBehindBatch(java.util.Map<String, Object> batchData,
                                                  Function<java.util.Map<String, Object>, java.util.Map<String, Object>> databaseBatchWriter,
                                                  long ttlSeconds, long delayMillis) {
        try {
            // 1. 立即更新缓存
            for (String key : batchData.keySet()) {
                Object value = batchData.get(key);
                if (value != null) {
                    writeToCache(key, value, ttlSeconds);
                }
            }
            
            log.debug("Write Behind批量: 缓存已更新，等待异步写入数据库: count={}", batchData.size());
            
            // 2. 异步延迟更新数据库
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            CompletableFuture.runAsync(() -> {
                try {
                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);
                    }
                    
                    java.util.Map<String, Object> results = databaseBatchWriter.apply(batchData);
                    
                    if (results != null && !results.isEmpty()) {
                        // 数据库更新成功后，再次更新缓存（确保一致性）
                        for (String key : results.keySet()) {
                            Object value = results.get(key);
                            if (value != null) {
                                writeToCache(key, value, ttlSeconds);
                            }
                        }
                        
                        log.debug("Write Behind批量: 数据库更新完成: count={}", results.size());
                    }
                    
                    future.complete(null);
                } catch (Exception e) {
                    log.error("Write Behind批量: 数据库更新失败: error={}", e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }, cacheExecutor);
            
            return future;
        } catch (Exception e) {
            log.error("Write Behind批量初始化失败: error={}", e.getMessage(), e);
            throw e;
        }
    }
}