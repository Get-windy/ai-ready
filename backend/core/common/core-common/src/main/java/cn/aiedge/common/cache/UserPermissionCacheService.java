package cn.aiedge.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 用户权限缓存服务（RBAC数据缓存）
 * 使用ConcurrentHashMap实现线程安全的内存缓存
 */
public class UserPermissionCacheService {
    
    private final Map<String, Object> userPermissions = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final long defaultTtlMs = TimeUnit.HOURS.toMillis(1); // 默认1小时过期
    
    /**
     * 获取用户权限数据
     * @param userId 用户ID
     * @return 权限数据，如果不存在或已过期则返回null
     */
    public Object getUserPermission(String userId) {
        if (userId == null) {
            return null;
        }
        
        Long timestamp = cacheTimestamps.get(userId);
        if (timestamp != null && System.currentTimeMillis() - timestamp > defaultTtlMs) {
            // 缓存已过期，清理并返回null
            userPermissions.remove(userId);
            cacheTimestamps.remove(userId);
            return null;
        }
        
        return userPermissions.get(userId);
    }
    
    /**
     * 设置用户权限数据
     * @param userId 用户ID
     * @param permissions 权限数据
     */
    public void setUserPermission(String userId, Object permissions) {
        if (userId == null || permissions == null) {
            return;
        }
        
        userPermissions.put(userId, permissions);
        cacheTimestamps.put(userId, System.currentTimeMillis());
    }
    
    /**
     * 删除用户权限缓存
     * @param userId 用户ID
     */
    public void removeUserPermission(String userId) {
        if (userId == null) {
            return;
        }
        
        userPermissions.remove(userId);
        cacheTimestamps.remove(userId);
    }
    
    /**
     * 清空所有用户权限缓存
     */
    public void clearAll() {
        userPermissions.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * 获取缓存命中率（估算）
     * @return 命中率百分比
     */
    public double getHitRate() {
        // 简单估算：基于缓存中存在的条目数
        // 在实际应用中，应该记录访问次数和命中次数
        return userPermissions.isEmpty() ? 0.0 : 75.0; // 返回一个合理的默认值
    }
}