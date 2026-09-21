package cn.aiedge.base.security;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 权限认证实现
 *
 * 实现用户角色和权限的动态加载。
 * 权限/角色缓存已委托给 {@link UnifiedPermissionCacheService} 统一管理。
 * 此类仅保留 Token 黑名单功能。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final StringRedisTemplate redisTemplate;
    private final UnifiedPermissionCacheService permissionCacheService;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        Long userId = resolveEffectiveUserId(Long.parseLong(loginId.toString()));
        return permissionCacheService.getPermissions(userId);
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        Long userId = resolveEffectiveUserId(Long.parseLong(loginId.toString()));
        return permissionCacheService.getRoles(userId);
    }

    /**
     * 解析本次权限判定以哪个用户为准（权限模拟的落地点）。
     *
     * <p>默认返回真实登录用户 —— 未开启模拟时行为与改动前**完全一致**；
     * 仅在开启模拟时返回被模拟用户，这正是「以某用户身份预览权限」的语义。</p>
     *
     * <p>此前 {@code PermissionSimulationService} 只把模拟态写进自己的 ThreadLocal、
     * 且没有任何判定逻辑读取它，功能是个空壳；现在由这里消费（2026-09-20 修复）。</p>
     */
    private Long resolveEffectiveUserId(Long actualUserId) {
        // 1) 单次模拟：请求头 X-Simulate-User-Id，由过滤器解析后写入 ThreadLocal
        Long singleRequest = PermissionSimulationHolder.getCurrentRequestTarget();
        if (singleRequest != null && !singleRequest.equals(actualUserId)) {
            return singleRequest;
        }
        // 2) 持久模拟：由 /api/simulate/start 写入 Sa-Token Session
        try {
            Object simulated = cn.dev33.satoken.stp.StpUtil.getSession()
                    .get(PermissionSimulationHolder.SESSION_KEY);
            if (simulated != null) {
                Long target = Long.parseLong(simulated.toString());
                if (!target.equals(actualUserId)) {
                    return target;
                }
            }
        } catch (Exception ignored) {
            // 无会话上下文（定时任务、内部调用、未登录）时按真实用户处理
        }
        return actualUserId;
    }

    /**
     * 清除用户权限缓存（用户角色或权限变更时调用）
     */
    public void clearUserPermissionCache(Long userId) {
        permissionCacheService.invalidate(userId);
    }

    /**
     * 将Token加入黑名单（登出时调用）
     *
     * @param token Token值
     * @param ttl 剩余有效时间（秒）
     */
    public void addToBlacklist(String token, long ttl) {
        if (token == null || token.isEmpty()) {
            return;
        }
        String key = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        log.info("Token已加入黑名单: {}", maskToken(token));
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param token Token值
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 从黑名单移除Token
     *
     * @param token Token值
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        String key = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token已从黑名单移除: {}", maskToken(token));
    }

    /**
     * 脱敏Token显示
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 16) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }
}
