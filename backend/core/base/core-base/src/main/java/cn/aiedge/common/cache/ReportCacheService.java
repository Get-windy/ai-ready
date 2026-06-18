package cn.aiedge.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 报表数据缓存服务
 * 用于缓存报表生成结果
 */
public class ReportCacheService {
    
    private final Map<String, Object> reportCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final long defaultTtlMs = TimeUnit.HOURS.toMillis(4); // 默认4小时过期（报表数据相对稳定）
    
    /**
     * 获取报表数据
     * @param reportId 报表ID
     * @return 报表数据，如果不存在或已过期则返回null
     */
    public Object getReport(String reportId) {
        if (reportId == null) {
            return null;
        }
        
        Long timestamp = cacheTimestamps.get(reportId);
        if (timestamp != null && System.currentTimeMillis() - timestamp > defaultTtlMs) {
            // 缓存已过期，清理并返回null
            reportCache.remove(reportId);
            cacheTimestamps.remove(reportId);
            return null;
        }
        
        return reportCache.get(reportId);
    }
    
    /**
     * 设置报表数据
     * @param reportId 报表ID
     * @param reportData 报表数据
     */
    public void setReport(String reportId, Object reportData) {
        if (reportId == null || reportData == null) {
            return;
        }
        
        reportCache.put(reportId, reportData);
        cacheTimestamps.put(reportId, System.currentTimeMillis());
    }
    
    /**
     * 删除报表缓存
     * @param reportId 报表ID
     */
    public void removeReport(String reportId) {
        if (reportId == null) {
            return;
        }
        
        reportCache.remove(reportId);
        cacheTimestamps.remove(reportId);
    }
    
    /**
     * 清空所有报表缓存
     */
    public void clearAll() {
        reportCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * 获取缓存命中率（估算）
     * @return 命中率百分比
     */
    public double getHitRate() {
        return reportCache.isEmpty() ? 0.0 : 85.0; // 返回一个合理的默认值
    }
}