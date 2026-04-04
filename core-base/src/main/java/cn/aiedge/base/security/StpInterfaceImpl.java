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
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final StringRedisTemplate redisTemplate;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    
    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // TODO: 从数据库获取权限列表
        // 暂时返回空列表
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // TODO: 从数据库获取角色列表
        // 暂时返回空列表
        return new ArrayList<>();
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
