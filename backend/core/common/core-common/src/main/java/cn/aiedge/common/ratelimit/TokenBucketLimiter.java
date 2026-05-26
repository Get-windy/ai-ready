package cn.aiedge.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流器（基于Redis实现分布式限流）
 * 
 * 算法原理：
 * 1. 按固定速率往桶里放令牌
 * 2. 请求来时从桶里取令牌
 * 3. 桶满时丢弃多余令牌
 * 4. 桶空时拒绝请求
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class TokenBucketLimiter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> rateLimitScript;
    
    private static final String KEY_PREFIX = "rate_limit:";
    private static final String SCRIPT = 
        "local key = KEYS[1]\n" +
        "local permits = tonumber(ARGV[1])\n" +
        "local rate = tonumber(ARGV[2])\n" +
        "local capacity = tonumber(ARGV[3])\n" +
        "local now = tonumber(ARGV[4])\n" +
        "\n" +
        "local info = redis.call('hmget', key, 'tokens', 'lastRefillTime')\n" +
        "local tokens = tonumber(info[1]) or capacity\n" +
        "local lastRefillTime = tonumber(info[2]) or now\n" +
        "\n" +
        "-- 计算需要补充的令牌数\n" +
        "local interval = now - lastRefillTime\n" +
        "local refillTokens = math.floor(interval * rate / 1000)\n" +
        "\n" +
        "-- 更新令牌数\n" +
        "tokens = math.min(capacity, tokens + refillTokens)\n" +
        "\n" +
        "-- 判断是否有足够令牌\n" +
        "local allowed = 0\n" +
        "if tokens >= permits then\n" +
        "    tokens = tokens - permits\n" +
        "    allowed = 1\n" +
        "end\n" +
        "\n" +
        "-- 更新Redis\n" +
        "redis.call('hmset', key, 'tokens', tokens, 'lastRefillTime', now)\n" +
        "redis.call('pexpire', key, 60000)\n" +
        "\n" +
        "return allowed";

    public TokenBucketLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>(SCRIPT, Long.class);
    }

    /**
     * 尝试获取令牌
     *
     * @param key      限流key
     * @param qps      每秒请求数（令牌生成速率）
     * @param capacity 桶容量
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, int qps, int capacity) {
        return tryAcquire(key, 1, qps, capacity);
    }

    /**
     * 尝试获取指定数量令牌
     *
     * @param key      限流key
     * @param permits  令牌数量
     * @param qps      每秒请求数
     * @param capacity 桶容量
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, int permits, int qps, int capacity) {
        String fullKey = KEY_PREFIX + key;
        long now = System.currentTimeMillis();
        
        try {
            Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(fullKey),
                String.valueOf(permits),
                String.valueOf(qps),
                String.valueOf(capacity),
                String.valueOf(now)
            );
            
            boolean allowed = result != null && result == 1;
            
            if (!allowed) {
                log.debug("限流拒绝: key={}, permits={}", key, permits);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("限流检查异常: key={}", key, e);
            // 降级：异常时允许通过
            return true;
        }
    }

    /**
     * 尝试获取令牌（带等待）
     *
     * @param key      限流key
     * @param permits  令牌数量
     * @param qps      每秒请求数
     * @param capacity 桶容量
     * @param timeout  超时时间（毫秒）
     * @return 是否获取成功
     */
    public boolean tryAcquireWithWait(String key, int permits, int qps, 
                                       int capacity, long timeout) {
        if (timeout <= 0) {
            return tryAcquire(key, permits, qps, capacity);
        }
        
        long deadline = System.currentTimeMillis() + timeout;
        
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire(key, permits, qps, capacity)) {
                return true;
            }
            
            // 等待一段时间后重试
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }

    /**
     * 获取当前可用令牌数
     *
     * @param key      限流key
     * @param capacity 桶容量
     * @return 可用令牌数
     */
    public long getAvailableTokens(String key, int capacity) {
        String fullKey = KEY_PREFIX + key;
        
        try {
            Object tokens = redisTemplate.opsForHash().get(fullKey, "tokens");
            return tokens != null ? ((Number) tokens).longValue() : capacity;
        } catch (Exception e) {
            log.error("获取令牌数异常: key={}", key, e);
            return capacity;
        }
    }

    /**
     * 重置限流器
     *
     * @param key 限流key
     */
    public void reset(String key) {
        String fullKey = KEY_PREFIX + key;
        redisTemplate.delete(fullKey);
    }
}
