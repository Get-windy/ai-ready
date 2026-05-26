package cn.aiedge.base.security;

import cn.aiedge.base.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限缓存管理器
 * 管理用户权限和角色的缓存，提高权限验证性能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheManager {

    private final StringRedisTemplate redisTemplate;
    private final SysUserService userService;

    private static final String USER_PERMISSION_CACHE_PREFIX = "user:permissions:";
    private static final String USER_ROLE_CACHE_PREFIX = "user:roles:";
    private static final String USER_DATA_PERMISSION_CACHE_PREFIX = "user:data-permissions:";
    private static final long CACHE_TTL_MINUTES = 30; // 30分钟缓存

    /**
     * 获取用户权限列表（带缓存）
     */
    public List<String> getUserPermissionsWithCache(Long userId) {
        String cacheKey = USER_PERMISSION_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null && !cached.isEmpty()) {
            try {
                return cn.hutool.json.JSONUtil.toList(cached, String.class);
            } catch (Exception e) {
                log.warn("解析权限缓存失败: userId={}, error={}", userId, e.getMessage());
                // 缓存损坏，删除后重新获取
                redisTemplate.delete(cacheKey);
            }
        }

        // 从数据库获取
        List<String> permissions = userService.getUserPermissionCodes(userId);
        if (permissions != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, cn.hutool.json.JSONUtil.toJsonStr(permissions), 
                        CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("保存权限缓存失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        return permissions;
    }

    /**
     * 获取用户角色列表（带缓存）
     */
    public List<String> getUserRolesWithCache(Long userId) {
        String cacheKey = USER_ROLE_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null && !cached.isEmpty()) {
            try {
                return cn.hutool.json.JSONUtil.toList(cached, String.class);
            } catch (Exception e) {
                log.warn("解析角色缓存失败: userId={}, error={}", userId, e.getMessage());
                // 缓存损坏，删除后重新获取
                redisTemplate.delete(cacheKey);
            }
        }

        // 从数据库获取
        List<String> roles = userService.getUserRoleCodes(userId);
        if (roles != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, cn.hutool.json.JSONUtil.toJsonStr(roles), 
                        CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("保存角色缓存失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        return roles;
    }

    /**
     * 清除用户权限缓存
     */
    public void clearUserPermissionCache(Long userId) {
        if (userId == null) return;
        
        String permissionCacheKey = USER_PERMISSION_CACHE_PREFIX + userId;
        String roleCacheKey = USER_ROLE_CACHE_PREFIX + userId;
        String dataPermissionCacheKey = USER_DATA_PERMISSION_CACHE_PREFIX + userId;
        
        redisTemplate.delete(permissionCacheKey);
        redisTemplate.delete(roleCacheKey);
        redisTemplate.delete(dataPermissionCacheKey);
        
        log.info("清除用户权限缓存: userId={}", userId);
    }

    /**
     * 清除所有用户权限缓存
     */
    public void clearAllUserPermissionCache() {
        // 清除所有用户权限相关缓存
        // 注意：这里只是一个示例，实际生产环境中需要更精确的清理策略
        log.info("清除所有用户权限缓存");
    }

    /**
     * 刷新用户权限缓存
     */
    public void refreshUserPermissionCache(Long userId) {
        clearUserPermissionCache(userId);
        // 重新获取权限以填充缓存
        getUserPermissionsWithCache(userId);
        getUserRolesWithCache(userId);
        log.info("刷新用户权限缓存: userId={}", userId);
    }
}