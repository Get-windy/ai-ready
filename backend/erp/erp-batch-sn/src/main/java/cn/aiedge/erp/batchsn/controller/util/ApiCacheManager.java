package cn.aiedge.erp.batchsn.controller.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * API缓存管理器
 * 用于管理批次和序列号API的缓存
 *
 * @author team-member
 * @date 2026-05-05
 */
@Component
@Slf4j
public class ApiCacheManager {

    private static final String BATCH_CACHE_PREFIX = "batch:";
    private static final String SERIAL_CACHE_PREFIX = "serial:";
    private static final String LIST_CACHE_PREFIX = "list:";

    // 使用ConcurrentHashMap作为本地缓存示例
    // 实际项目中应该使用Redis等分布式缓存
    private final ConcurrentHashMap<String, Object> localCache = new ConcurrentHashMap<>();

    /**
     * 获取批次缓存键
     */
    private String getBatchCacheKey(Long batchId) {
        return BATCH_CACHE_PREFIX + batchId;
    }

    /**
     * 获取序列号缓存键
     */
    private String getSerialCacheKey(Long serialId) {
        return SERIAL_CACHE_PREFIX + serialId;
    }

    /**
     * 获取列表缓存键
     */
    private String getListCacheKey(String queryHash) {
        return LIST_CACHE_PREFIX + queryHash;
    }

    /**
     * 缓存批次信息
     */
    @Cacheable(value = "batch", key = "#batchId")
    public <T> T getBatchCache(Long batchId) {
        String cacheKey = getBatchCacheKey(batchId);
        return (T) localCache.get(cacheKey);
    }

    /**
     * 设置批次缓存
     */
    public void setBatchCache(Long batchId, Object data) {
        String cacheKey = getBatchCacheKey(batchId);
        localCache.put(cacheKey, data);
        log.debug("批次缓存已设置: {} -> {}", cacheKey, data.getClass().getSimpleName());
    }

    /**
     * 清除批次缓存
     */
    @CacheEvict(value = "batch", key = "#batchId")
    public void evictBatchCache(Long batchId) {
        String cacheKey = getBatchCacheKey(batchId);
        localCache.remove(cacheKey);
        log.debug("批次缓存已清除: {}", cacheKey);
    }

    /**
     * 缓存序列号信息
     */
    @Cacheable(value = "serial", key = "#serialId")
    public <T> T getSerialCache(Long serialId) {
        String cacheKey = getSerialCacheKey(serialId);
        return (T) localCache.get(cacheKey);
    }

    /**
     * 设置序列号缓存
     */
    public void setSerialCache(Long serialId, Object data) {
        String cacheKey = getSerialCacheKey(serialId);
        localCache.put(cacheKey, data);
        log.debug("序列号缓存已设置: {} -> {}", cacheKey, data.getClass().getSimpleName());
    }

    /**
     * 清除序列号缓存
     */
    @CacheEvict(value = "serial", key = "#serialId")
    public void evictSerialCache(Long serialId) {
        String cacheKey = getSerialCacheKey(serialId);
        localCache.remove(cacheKey);
        log.debug("序列号缓存已清除: {}", cacheKey);
    }

    /**
     * 缓存查询列表
     */
    public <T> T getListCache(String queryHash) {
        String cacheKey = getListCacheKey(queryHash);
        return (T) localCache.get(cacheKey);
    }

    /**
     * 设置查询列表缓存
     */
    public void setListCache(String queryHash, Object data) {
        String cacheKey = getListCacheKey(queryHash);
        localCache.put(cacheKey, data);
        log.debug("列表缓存已设置: {} -> {}", cacheKey, data.getClass().getSimpleName());
    }

    /**
     * 清除所有查询列表缓存
     */
    public void clearAllListCache() {
        localCache.keySet().removeIf(key -> key.startsWith(LIST_CACHE_PREFIX));
        log.debug("所有列表缓存已清除");
    }

    /**
     * 清除所有批次相关缓存
     */
    public void clearAllBatchCache() {
        localCache.keySet().removeIf(key -> key.startsWith(BATCH_CACHE_PREFIX));
        log.debug("所有批次缓存已清除");
    }

    /**
     * 清除所有序列号相关缓存
     */
    public void clearAllSerialCache() {
        localCache.keySet().removeIf(key -> key.startsWith(SERIAL_CACHE_PREFIX));
        log.debug("所有序列号缓存已清除");
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        localCache.clear();
        log.debug("所有缓存已清除");
    }

    /**
     * 生成查询哈希值
     */
    public String generateQueryHash(Object queryObject) {
        if (queryObject == null) {
            return "empty";
        }
        return Integer.toHexString(queryObject.hashCode());
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        long batchCount = localCache.keySet().stream()
                .filter(key -> key.startsWith(BATCH_CACHE_PREFIX))
                .count();
        long serialCount = localCache.keySet().stream()
                .filter(key -> key.startsWith(SERIAL_CACHE_PREFIX))
                .count();
        long listCount = localCache.keySet().stream()
                .filter(key -> key.startsWith(LIST_CACHE_PREFIX))
                .count();

        return CacheStats.builder()
                .totalSize(localCache.size())
                .batchCacheSize(batchCount)
                .serialCacheSize(serialCount)
                .listCacheSize(listCount)
                .build();
    }

    /**
     * 缓存统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStats {
        private long totalSize;
        private long batchCacheSize;
        private long serialCacheSize;
        private long listCacheSize;
    }
}