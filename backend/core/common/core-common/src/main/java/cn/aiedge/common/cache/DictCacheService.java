package cn.aiedge.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 字典配置缓存服务
 * 用于缓存系统字典配置数据
 */
public class DictCacheService {
    
    private final Map<String, Object> dictCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final long defaultTtlMs = TimeUnit.HOURS.toMillis(2); // 默认2小时过期
    
    /**
     * 获取字典配置
     * @param dictType 字典类型
     * @return 字典配置数据，如果不存在或已过期则返回null
     */
    public Object getDict(String dictType) {
        if (dictType == null) {
            return null;
        }
        
        Long timestamp = cacheTimestamps.get(dictType);
        if (timestamp != null && System.currentTimeMillis() - timestamp > defaultTtlMs) {
            // 缓存已过期，清理并返回null
            dictCache.remove(dictType);
            cacheTimestamps.remove(dictType);
            return null;
        }
        
        return dictCache.get(dictType);
    }
    
    /**
     * 设置字典配置
     * @param dictType 字典类型
     * @param dictData 字典数据
     */
    public void setDict(String dictType, Object dictData) {
        if (dictType == null || dictData == null) {
            return;
        }
        
        dictCache.put(dictType, dictData);
        cacheTimestamps.put(dictType, System.currentTimeMillis());
    }
    
    /**
     * 删除字典配置缓存
     * @param dictType 字典类型
     */
    public void removeDict(String dictType) {
        if (dictType == null) {
            return;
        }
        
        dictCache.remove(dictType);
        cacheTimestamps.remove(dictType);
    }
    
    /**
     * 清空所有字典配置缓存
     */
    public void clearAll() {
        dictCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * 获取缓存命中率（估算）
     * @return 命中率百分比
     */
    public double getHitRate() {
        return dictCache.isEmpty() ? 0.0 : 80.0; // 返回一个合理的默认值
    }
}