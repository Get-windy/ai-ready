package cn.aiedge.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 * 负责管理被拉黑的Token，防止Token重放攻击
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private StringRedisTemplate redisTemplate;
    
    // Token黑名单前缀
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    // 默认黑名单过期时间（与Token有效期一致）
    private static final long DEFAULT_EXPIRE_SECONDS = 30 * 60; // 30分钟

    public TokenBlacklistService() {
    }
    
    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将Token加入黑名单
     * 
     * @param token Token值
     */
    public void addToBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "true", DEFAULT_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.info("Token已加入黑名单: {}", token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            log.error("将Token加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 将Token加入黑名单（指定过期时间）
     * 
     * @param token Token值
     * @param expireSeconds 过期时间（秒）
     */
    public void addToBlacklist(String token, long expireSeconds) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "true", expireSeconds, TimeUnit.SECONDS);
            log.info("Token已加入黑名单（{}秒后过期）: {}", expireSeconds, 
                    token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            log.error("将Token加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查Token是否在黑名单中
     * 
     * @param token Token值
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return true; // 空Token视为无效
        }
        
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查Token黑名单失败: {}", e.getMessage());
            return true; // 异常情况下视为无效
        }
    }

    /**
     * 从黑名单中移除Token
     * 
     * @param token Token值
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("Token已从黑名单移除: {}", token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            log.error("从黑名单移除Token失败: {}", e.getMessage());
        }
    }

    /**
     * 清理过期的黑名单记录（通常由Redis自动处理）
     */
    public void cleanupExpiredTokens() {
        // Redis会自动清理过期的key，此方法留作扩展使用
        log.debug("Token黑名单清理任务执行");
    }
}