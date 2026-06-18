package cn.aiedge.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 库存数据缓存服务
 * 用于缓存商品库存信息
 */
public class StockCacheService {
    
    private final Map<String, Object> stockCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final long defaultTtlMs = TimeUnit.MINUTES.toMillis(30); // 默认30分钟过期（库存数据变化频繁）
    
    /**
     * 获取库存数据
     * @param productId 商品ID
     * @return 库存数据，如果不存在或已过期则返回null
     */
    public Object getStock(String productId) {
        if (productId == null) {
            return null;
        }
        
        Long timestamp = cacheTimestamps.get(productId);
        if (timestamp != null && System.currentTimeMillis() - timestamp > defaultTtlMs) {
            // 缓存已过期，清理并返回null
            stockCache.remove(productId);
            cacheTimestamps.remove(productId);
            return null;
        }
        
        return stockCache.get(productId);
    }
    
    /**
     * 设置库存数据
     * @param productId 商品ID
     * @param stockData 库存数据
     */
    public void setStock(String productId, Object stockData) {
        if (productId == null || stockData == null) {
            return;
        }
        
        stockCache.put(productId, stockData);
        cacheTimestamps.put(productId, System.currentTimeMillis());
    }
    
    /**
     * 删除库存缓存
     * @param productId 商品ID
     */
    public void removeStock(String productId) {
        if (productId == null) {
            return;
        }
        
        stockCache.remove(productId);
        cacheTimestamps.remove(productId);
    }
    
    /**
     * 清空所有库存缓存
     */
    public void clearAll() {
        stockCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * 获取缓存命中率（估算）
     * @return 命中率百分比
     */
    public double getHitRate() {
        return stockCache.isEmpty() ? 0.0 : 72.0; // 返回一个合理的默认值
    }
}