package cn.aiedge.erp.batchsn.cache;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 批次缓存服务接口
 *
 * @author team-member
 * @date 2026-05-01
 */
public interface BatchCacheService {
    
    /**
     * 根据ID获取批次详情（带缓存）
     */
    @Nullable
    BatchNumber getBatchById(Long batchId);
    
    /**
     * 批量获取批次详情
     */
    Map<Long, BatchNumber> batchGetBatches(List<Long> batchIds);
    
    /**
     * 根据批次号获取批次详情（带缓存）
     */
    @Nullable
    BatchNumber getBatchByNo(String batchNo);
    
    /**
     * 缓存批次详情
     */
    void cacheBatchDetail(BatchNumber batch);
    
    /**
     * 批量缓存批次详情
     */
    void batchCacheBatches(List<BatchNumber> batches);
    
    /**
     * 删除批次详情缓存
     */
    void deleteBatchDetailCache(Long batchId);
    
    /**
     * 批量删除批次详情缓存
     */
    void batchDeleteBatchDetailCache(List<Long> batchIds);
    
    /**
     * 缓存批次列表查询结果
     */
    void cacheBatchList(String cacheKey, List<BatchNumber> batches);
    
    /**
     * 获取批次列表缓存
     */
    List<BatchNumber> getBatchListCache(String cacheKey);
    
    /**
     * 删除批次列表缓存
     */
    void deleteBatchListCache(String cacheKey);
    
    /**
     * 删除所有批次列表缓存
     */
    void deleteAllBatchListCaches();
    
    /**
     * 缓存库存汇总数据
     */
    void cacheStockSummary(Long warehouseId, List<BatchNumber> summary);
    
    /**
     * 获取库存汇总缓存
     */
    List<BatchNumber> getStockSummaryCache(Long warehouseId);
    
    /**
     * 删除库存汇总缓存
     */
    void deleteStockSummaryCache(Long warehouseId);
    
    /**
     * 删除所有库存汇总缓存
     */
    void deleteAllStockSummaryCaches();
    
    /**
     * 缓存临期预警数据
     */
    void cacheExpiringWarning(int warningDays, List<BatchNumber> batches);
    
    /**
     * 获取临期预警缓存
     */
    List<BatchNumber> getExpiringWarningCache(int warningDays);
    
    /**
     * 删除临期预警缓存
     */
    void deleteExpiringWarningCache(int warningDays);
    
    /**
     * 缓存空值（防止缓存穿透）
     */
    void cacheNullValue(Long batchId);
    
    /**
     * 检查是否是空值缓存
     */
    boolean isNullValueCached(Long batchId);
    
    /**
     * 获取缓存命中率统计
     */
    CacheStats getCacheStats();
    
    /**
     * 清空所有缓存
     */
    void clearAllCaches();
    
    /**
     * 异步缓存批次详情
     */
    void asyncCacheBatchDetail(Long batchId);
    
    /**
     * 异步批量缓存批次详情
     */
    void asyncBatchCacheBatches(List<Long> batchIds);
    
    /**
     * 获取分布式锁
     */
    boolean acquireLock(String lockKey, long ttlSeconds);
    
    /**
     * 释放分布式锁
     */
    boolean releaseLock(String lockKey);
    
    /**
     * 生成批次列表缓存键
     */
    String generateBatchListCacheKey(String batchNo, String productCode, String status, 
                                     String sourceType, int page, int size);
    
    /**
     * 获取可用数量缓存
     */
    BigDecimal getAvailableQuantityCache(Long batchId);
    
    /**
     * 缓存可用数量
     */
    void cacheAvailableQuantity(Long batchId, BigDecimal quantity);
    
    /**
     * 删除可用数量缓存
     */
    void deleteAvailableQuantityCache(Long batchId);
    
    /**
     * 获取批次号存在性缓存
     */
    Boolean getBatchNoExistsCache(String batchNo);
    
    /**
     * 缓存批次号存在性结果
     */
    void cacheBatchNoExists(String batchNo, boolean exists);
    
    /**
     * 删除批次号存在性缓存
     */
    void deleteBatchNoExistsCache(String batchNo);
}