package cn.aiedge.base.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 登录尝试服务
 * <p>
 * 基于 Redis 记录登录失败次数，超过阈值时临时锁定账户。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    /** 最大连续失败次数 */
    public static final int MAX_ATTEMPTS = 5;

    /** 锁定时间（分钟） */
    public static final long LOCK_DURATION_MINUTES = 15;

    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    /**
     * 记录登录失败
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @return 当前连续失败次数
     */
    public int recordFailedAttempt(String username, Long tenantId) {
        String key = buildFailKey(username, tenantId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) count = 1L;

        // 设置首次写入后的过期时间（防止长期不登录的残留 key）
        if (count == 1) {
            redisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        // 超过阈值时锁定
        if (count >= MAX_ATTEMPTS) {
            lockAccount(username, tenantId);
        }

        log.warn("登录失败: username={}, tenantId={}, attempts={}/{}",
                username, tenantId, count, MAX_ATTEMPTS);
        return count.intValue();
    }

    /**
     * 登录成功后清除失败记录
     */
    public void clearFailedAttempts(String username, Long tenantId) {
        String failKey = buildFailKey(username, tenantId);
        String lockKey = buildLockKey(username, tenantId);
        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);
    }

    /**
     * 检查账户是否被锁定
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @return true 如果账户被锁定
     */
    public boolean isLocked(String username, Long tenantId) {
        String lockKey = buildLockKey(username, tenantId);
        Boolean hasKey = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(hasKey);
    }

    /**
     * 获取锁定剩余时间（秒），-1 表示未锁定
     */
    public long getLockRemainingSeconds(String username, Long tenantId) {
        String lockKey = buildLockKey(username, tenantId);
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : -1;
    }

    /**
     * 锁定账户
     */
    private void lockAccount(String username, Long tenantId) {
        String lockKey = buildLockKey(username, tenantId);
        redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        log.warn("账户已被临时锁定: username={}, tenantId={}, duration={}分钟",
                username, tenantId, LOCK_DURATION_MINUTES);
    }

    /**
     * 获取当前失败次数（不触发锁定）
     */
    public int getAttemptCount(String username, Long tenantId) {
        String key = buildFailKey(username, tenantId);
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }

    private String buildFailKey(String username, Long tenantId) {
        return FAIL_PREFIX + tenantId + ":" + username;
    }

    private String buildLockKey(String username, Long tenantId) {
        return LOCK_PREFIX + tenantId + ":" + username;
    }
}
