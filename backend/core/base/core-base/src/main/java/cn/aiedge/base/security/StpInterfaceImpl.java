package cn.aiedge.base.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 权限认证实现
 * 实现用户角色和权限的动态加载
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final StringRedisTemplate redisTemplate;
    private final cn.aiedge.base.service.SysUserService userService;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String PERMISSION_CACHE_PREFIX = "user:permission:";
    private static final String ROLE_CACHE_PREFIX = "user:role:";
    private static final long CACHE_TTL_SECONDS = 300; // 5分钟缓存
    
    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        
        Long userId = Long.parseLong(loginId.toString());
        String cacheKey = PERMISSION_CACHE_PREFIX + userId;
        
        // 先从缓存获取
        List<String> cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库获取
        List<String> permissions = userService.getUserPermissionCodes(userId);
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        
        // 写入缓存
        saveToCache(cacheKey, permissions);
        
        log.debug("获取用户权限列表: userId={}, permissions={}", userId, permissions.size());
        return permissions;
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        
        Long userId = Long.parseLong(loginId.toString());
        String cacheKey = ROLE_CACHE_PREFIX + userId;
        
        // 先从缓存获取
        List<String> cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库获取
        List<String> roles = userService.getUserRoleCodes(userId);
        if (roles == null) {
            roles = new ArrayList<>();
        }
        
        // 写入缓存
        saveToCache(cacheKey, roles);
        
        log.debug("获取用户角色列表: userId={}, roles={}", userId, roles.size());
        return roles;
    }
    
    /**
     * 清除用户权限缓存（用户角色或权限变更时调用）
     */
    public void clearUserPermissionCache(Long userId) {
        if (userId == null) return;
        redisTemplate.delete(PERMISSION_CACHE_PREFIX + userId);
        redisTemplate.delete(ROLE_CACHE_PREFIX + userId);
        log.info("清除用户权限缓存: userId={}", userId);
    }
    
    /**
     * 从缓存获取列表
     */
    private List<String> getFromCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isEmpty()) {
                return cn.hutool.json.JSONUtil.toList(json, String.class);
            }
        } catch (Exception e) {
            log.warn("读取权限缓存失败: key={}, error={}", key, e.getMessage());
        }
        return null;
    }
    
    /**
     * 保存列表到缓存
     */
    private void saveToCache(String key, List<String> list) {
        try {
            redisTemplate.opsForValue().set(key, cn.hutool.json.JSONUtil.toJsonStr(list), 
                    CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入权限缓存失败: key={}, error={}", key, e.getMessage());
        }
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
