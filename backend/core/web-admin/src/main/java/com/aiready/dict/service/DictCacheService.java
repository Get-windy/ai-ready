package com.aiready.dict.service;

import com.aiready.dict.dto.DictImportExportDTO;
import com.aiready.dict.dto.DictTypeDTO;

import java.util.List;

/**
 * 字典缓存服务接口
 */
public interface DictCacheService {
    
    /**
     * 获取字典项缓存
     */
    String getCacheKey(String dictCode);
    
    /**
     * 缓存字典项列表
     */
    void cacheDictItems(String dictCode, List<?> items);
    
    /**
     * 获取缓存的字典项列表
     */
    Object getCachedDictItems(String dictCode);
    
    /**
     * 清除字典缓存
     */
    void evictDictCache(String dictCode);
    
    /**
     * 清除所有字典缓存
     */
    void evictAllDictCache();
    
    /**
     * 刷新字典缓存
     */
    void refreshDictCache(String dictCode);
    
    /**
     * 刷新所有字典缓存
     */
    void refreshAllDictCache();
}
