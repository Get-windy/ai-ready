package cn.aiedge.base.event;

import cn.aiedge.base.security.UnifiedPermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统一权限缓存清除器
 * 提供清除指定用户或全部用户权限缓存的能力
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedPermissionCacheEvictor {

    private final UnifiedPermissionCacheService cacheService;

    /**
     * 清除指定用户的权限缓存
     *
     * @param userId 用户ID
     */
    public void evictUser(Long userId) {
        try {
            cacheService.evictUserCache(userId);
            log.debug("[缓存清除] 已清除用户 {} 的权限缓存", userId);
        } catch (Exception e) {
            log.warn("[缓存清除] 清除用户 {} 缓存失败: {}", userId, e.getMessage());
        }
    }

    /**
     * 清除所有用户的权限缓存
     */
    public void evictAll() {
        try {
            cacheService.evictAllCache();
            log.info("[缓存清除] 已清除所有用户的权限缓存");
        } catch (Exception e) {
            log.warn("[缓存清除] 清除所有缓存失败: {}", e.getMessage());
        }
    }
}
