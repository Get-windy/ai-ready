package cn.aiedge.base.security;

import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.websocket.SseNotificationService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 统一权限缓存服务
 *
 * 两级缓存策略:
 *   L1: Caffeine 本地缓存（30秒 TTL，用于高频读取，避免 Redis 压力）
 *   L2: Redis 集中缓存（5分钟 TTL，用于分布式环境下的缓存共享）
 *
 * 缓存失效机制:
 *   1. 权限变更 → 删除 Redis 缓存
 *   2. 通过 Redis Pub/Sub 通知所有实例
 *   3. 各实例收到通知后清空本地 Caffeine 缓存
 *
 * 废弃的旧缓存:
 *   - StpInterfaceImpl 中的 5min Redis 缓存 → 由此服务统一管理
 *   - PermissionCacheManager 30min 缓存 → 由此服务统一管理
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedPermissionCacheService {

    private final StringRedisTemplate redisTemplate;
    private final SysUserService userService;
    private final SseNotificationService sseService;

    // 可选：Redis listener 用于接收缓存失效通知
    private RedisMessageListenerContainer listenerContainer;

    // L1: Caffeine 本地缓存（30秒 TTL，最多 10000 个用户）
    private final Cache<Long, List<String>> permissionLocalCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .recordStats()
            .build();

    private final Cache<Long, List<String>> roleLocalCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .recordStats()
            .build();

    private static final String PERMISSION_REDIS_PREFIX = "up:perms:";
    private static final String ROLE_REDIS_PREFIX = "up:roles:";
    private static final long REDIS_TTL_SECONDS = 300; // 5分钟
    private static final String CACHE_CHANNEL = "permission:cache:change";

    /**
     * 获取用户权限列表
     */
    public List<String> getPermissions(Long userId) {
        if (userId == null) return new ArrayList<>();

        // L1: 本地缓存
        List<String> perms = permissionLocalCache.getIfPresent(userId);
        if (perms != null) {
            return perms;
        }

        // L2: Redis 缓存
        String redisKey = PERMISSION_REDIS_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null && !json.isEmpty()) {
            try {
                perms = cn.hutool.json.JSONUtil.toList(json, String.class);
                permissionLocalCache.put(userId, perms);
                return perms;
            } catch (Exception e) {
                log.warn("解析权限 Redis 缓存失败: userId={}, error={}", userId, e.getMessage());
                redisTemplate.delete(redisKey);
            }
        }

        // 数据库查询
        perms = userService.getUserPermissionCodes(userId);
        if (perms == null) {
            perms = new ArrayList<>();
        }

        // 写入 Redis 缓存
        final List<String> finalPerms = perms;
        try {
            redisTemplate.opsForValue().set(redisKey,
                    cn.hutool.json.JSONUtil.toJsonStr(finalPerms),
                    REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入权限 Redis 缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        // 写入本地缓存
        permissionLocalCache.put(userId, finalPerms);

        return finalPerms;
    }

    /**
     * 获取用户角色列表
     */
    public List<String> getRoles(Long userId) {
        if (userId == null) return new ArrayList<>();

        // L1: 本地缓存
        List<String> roles = roleLocalCache.getIfPresent(userId);
        if (roles != null) {
            return roles;
        }

        // L2: Redis 缓存
        String redisKey = ROLE_REDIS_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null && !json.isEmpty()) {
            try {
                roles = cn.hutool.json.JSONUtil.toList(json, String.class);
                roleLocalCache.put(userId, roles);
                return roles;
            } catch (Exception e) {
                log.warn("解析角色 Redis 缓存失败: userId={}, error={}", userId, e.getMessage());
                redisTemplate.delete(redisKey);
            }
        }

        // 数据库查询
        roles = userService.getUserRoleCodes(userId);
        if (roles == null) {
            roles = new ArrayList<>();
        }

        // 写入 Redis 缓存
        final List<String> finalRoles = roles;
        try {
            redisTemplate.opsForValue().set(redisKey,
                    cn.hutool.json.JSONUtil.toJsonStr(finalRoles),
                    REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入角色 Redis 缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        // 写入本地缓存
        roleLocalCache.put(userId, finalRoles);

        return finalRoles;
    }

    /**
     * 使指定用户的权限缓存失效（权限变更时调用）
     */
    public void invalidate(Long userId) {
        if (userId == null) return;

        // 清除本地缓存
        permissionLocalCache.invalidate(userId);
        roleLocalCache.invalidate(userId);

        // 清除 Redis 缓存
        redisTemplate.delete(PERMISSION_REDIS_PREFIX + userId);
        redisTemplate.delete(ROLE_REDIS_PREFIX + userId);

        // 广播失效通知到其他实例
        try {
            redisTemplate.convertAndSend(CACHE_CHANNEL, userId.toString());
        } catch (Exception e) {
            log.warn("发送缓存失效广播失败: userId={}, error={}", userId, e.getMessage());
        }

        log.info("权限缓存已失效: userId={}", userId);

        // 通过 SSE 通知该用户的前端页面立即刷新权限缓存
        sseService.notifyCacheInvalidation(userId);
    }

    /**
     * 刷新用户权限缓存（清除后重新加载）
     */
    public List<String> refreshPermissions(Long userId) {
        invalidate(userId);
        return getPermissions(userId);
    }

    /**
     * 刷新用户角色缓存
     */
    public List<String> refreshRoles(Long userId) {
        invalidate(userId);
        return getRoles(userId);
    }

    /**
     * 获取缓存统计信息
     */
    public String getStats() {
        return String.format(
            "PermissionCache: hitRate=%.2f, evictions=%d, size=%d | RoleCache: hitRate=%.2f, evictions=%d, size=%d",
            permissionLocalCache.stats().hitRate(),
            permissionLocalCache.stats().evictionCount(),
            permissionLocalCache.estimatedSize(),
            roleLocalCache.stats().hitRate(),
            roleLocalCache.stats().evictionCount(),
            roleLocalCache.estimatedSize()
        );
    }

    @PostConstruct
    public void init() {
        log.info("统一权限缓存服务已初始化 (L1: Caffeine 30s, L2: Redis 5min)");
    }

    @PreDestroy
    public void destroy() {
        permissionLocalCache.invalidateAll();
        roleLocalCache.invalidateAll();
        log.info("统一权限缓存服务已关闭，本地缓存已清空");
    }
}
